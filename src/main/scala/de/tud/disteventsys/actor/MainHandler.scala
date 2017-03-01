package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import de.tud.disteventsys.actor.HelperHandler.Messages.{HelperResponse, StartOperation}
import ScheduleActor.Messages._
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}

import scala.util.Random
import scala.concurrent.duration._

/**
  * Created by ms on 07.02.17.
  */

object MainHandler{
  def props(originalSender: ActorRef, actors: Map[String, ActorRef], eventsList: List[String],
            hHandler: Option[ActorRef], actionEvent: String, eventTimeoutDuration: FiniteDuration): Props = {
    Props(new MainHandler(originalSender, actors, eventsList, hHandler, actionEvent, eventTimeoutDuration))
  }
}

class MainHandler(originalSender: ActorRef, actors: Map[String, ActorRef], eventsList: List[String],
                  hHandler: Option[ActorRef], actionEvent: String,
                  eventTimeoutDuration: FiniteDuration) extends Actor with ActorLogging {

  private final val eventsCount = eventsList.length
  //private var eventsFired: Array[_] = Array.empty
  //private var receivedEvents: List[Option[ActorRef]] = List.empty
  private var receivedEventsWithIds: List[Tuple3[Long, Option[AnyRef], Option[_]]] = List.empty
  val rand = new Random()
  val scheduler = context.actorOf(ScheduleActor.props, "scheduler")
  var eventCount, fulfilledCount, timeoutCount = 0
  var timeoutsReceived: List[Long] = List.empty

  def receive = LoggingReceive {
    case EsperEvent(clz, underlying) =>
      val actor = getActor(underlying)
      println(s"ESPER EVENT IN HANDLEr: $clz $underlying")
      eventCount += 1
      handleIncomingEvent(actor, underlying)
    case HelperResponse(id, response) =>
      // check if timeout received for event before fulfilling
      if(!timeoutsReceived.contains(id)){
        fulfilledCount += 1
        handleHelperResponse(id, response)
      }
    case Timeout(id) =>
      timeoutsReceived = timeoutsReceived :+ id
      timeoutCount += 1
      log.info(s"MAIN HANDLER: Timeout received for $id")
      handleTimeout(id)
  }

  private def handleIncomingEvent(actor: ActorRef, underlying: AnyRef) = {
    val long = rand.nextLong()
    actor ! underlying
    receivedEventsWithIds = receivedEventsWithIds :+ Tuple3(long, Some(underlying), None)
    sendToHelperHandler(long, underlying)
    scheduler ! StartTimer(long, eventTimeoutDuration)
  }

  private def sendToHelperHandler(id: Long, evt: AnyRef) = {
    hHandler.map { handler => handler ! StartOperation(id, evt) }
  }
  // find event by id and handle error
  private def handleTimeout(id: Long) = {
    //val evtWithId = (receivedEventsWithIds find { case (ind, Some(evt), None) => ind == id }).get
    val evtWithId = receivedEventsWithIds find { case (ind, Some(evt), None) => ind == id }
    log.info(s"MAIN HANDLER: Timeout for evtWithId, $evtWithId")
    log.info(s"MAIN HANDLER: events, fulfilled, timeout counts: $eventCount, $fulfilledCount, $timeoutCount")
    // TODO: remove event from list?
  }

  // find Tuple3 by id and add response, then collect
  private def handleHelperResponse(id: Long, response: AnyRef) = {
    // tell schedule handler actor to stop timeout for this id
    scheduler ! StopTimeout(id)
    val evtWithId = (receivedEventsWithIds map {
      case (ind, undly, None) => if(ind == id) (ind, undly, Some(response))
    }).head
    log.info(s"MAIN HANDLER: Response In: $evtWithId")
    log.info(s"MAIN HANDLER: events, fulfilled, timeout counts: $eventCount, $fulfilledCount, $timeoutCount")
    removeFulfilledEvents(id)
    // TODO: send response to EsperActor?
  }

  // reset receivedEventsWithIds to values excluding fulfilled events
  private def removeFulfilledEvents(id: Long) = {
    receivedEventsWithIds = receivedEventsWithIds filter { case (ind, underly, _) => ind != id }
    log.info(s"REMOVED Fulfilled event: $id")
  }

  private def getActor(underlying: AnyRef): ActorRef = {
    def getter(name: String) = { actors.get(name) match { case Some(actor) => actor } }
    underlying match {
      case Sell(s, p, a) => getter("seller")
      case Buy(s, p, a)  => getter("buyer")
      case Price(s, p)   => getter("pricer")
    }
  }

  //import context.dispatcher
  //val timeoutMessage = context.system.scheduler.scheduleOnce(delay){self ! RequestTimeout}
}