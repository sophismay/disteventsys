package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import de.tud.disteventsys.actor.HelperHandler.Messages.HelperResponse
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}

import scala.util.Random
import scala.concurrent.duration._

/**
  * Created by ms on 07.02.17.
  */

object MainHandler{
  object Messages {
    sealed abstract class HandlerMessage
    case object HandlerResponse                                                  extends HandlerMessage
    case class HandlerResponseResult(response: Tuple2[Option[Any], Option[Any]]) extends HandlerMessage
    case object RequestTimeout                                                   extends HandlerMessage
    case object SecondHandlerResponse                                            extends HandlerMessage
  }

  def props(originalSender: ActorRef, actors: Map[String, ActorRef], eventsList: List[String],
            hHandler: Option[ActorRef], actionEvent: String, eventTimeoutDuration: FiniteDuration): Props = {
    Props(new MainHandler(originalSender, actors, eventsList, hHandler, actionEvent, eventTimeoutDuration))
  }
}

class MainHandler(originalSender: ActorRef, actors: Map[String, ActorRef], eventsList: List[String],
                  hHandler: Option[ActorRef], actionEvent: String,
                  eventTimeoutDuration: FiniteDuration) extends Actor with ActorLogging {
  import MainHandler.Messages._
  import ScheduleActor.Messages._

  private final val eventsCount = eventsList.length
  //private var eventsFired: Array[_] = Array.empty
  //private var receivedEvents: List[Option[ActorRef]] = List.empty
  private var receivedEventsWithIds: List[Tuple3[Long, Option[AnyRef], Option[_]]] = List.empty
  val rand = new Random()
  val scheduler = context.actorOf(ScheduleActor.props, "scheduler")

  def receive = LoggingReceive {
    case EsperEvent(clz, underlying) =>
      val actor = getActor(underlying)
      println(s"ESPER EVENT IN HANDLEr: $clz $underlying")
      val long = rand.nextLong()
      underlying match {
        case Buy(s, p, a)  =>
          actor ! Buy(s, p, a)
          receivedEventsWithIds = receivedEventsWithIds :+ Tuple3(long, Some(underlying), None)
          scheduler ! StartTimer(long, eventTimeoutDuration)
          handleFiredEvent(long, Buy(s, p, a))
        case Price(s, p)   =>
          actor ! Price(s, p)
          receivedEventsWithIds = receivedEventsWithIds :+ Tuple3(long, Some(underlying), None)
          scheduler ! StartTimer(long, eventTimeoutDuration)
          handleFiredEvent(long, Price(s, p))
        case Sell(s, p, a) =>
          actor ! Sell(s, p, a)
          receivedEventsWithIds = receivedEventsWithIds :+ Tuple3(long, Some(underlying), None)
          scheduler ! StartTimer(long, eventTimeoutDuration)
          handleFiredEvent(long, Sell(s, p, a))
      }
    case HelperResponse(id, response) =>
      handleHelperResponse(id, response)
      //collectResponse(id, response)
    /*case HandlerResponse =>
      // add response to resultsFired, then collectResponse
      collectResponse*/
    case RequestTimeout  =>
      println("TIMEOUT RECEIVED: ")
      sendResponseAndShutdown(RequestTimeout)
  }

  private def handleFiredEvent(id: Long, evt: AnyRef) = {
    //eventsFired = eventsFired :+ evt
    // collect response
    //collectResponse
  }
  // find Tuple3 by id and add response, then collect
  private def handleHelperResponse(id: Long, response: AnyRef) = {
    val evtWithId = (receivedEventsWithIds map {
      case (ind, undly, None) => if(ind == id) (ind, undly, Some(response))
    }).head
    log.info(s"MAIN HANDLER: Response In: $evtWithId")
    removeFulfilledEvents(id)
    // TODO: send response to EsperActor?
  }

  // reset receivedEventsWithIds to values excluding fulfilled events
  private def removeFulfilledEvents(id: Long) = {
    receivedEventsWithIds = receivedEventsWithIds filter { case (ind, underly, _) => ind != id }
  }

  private def startTimerForEvent(id: Long) = {

  }
  // check and collect response
  // after receiving response from helper Handler(which handles the action event [from stream])
  // remove successfully completed pair events in the end
  private def collectResponse(id: Long, response: AnyRef) = {


    /*var gath: Array[String] = Array.empty
    val gathered = for {
      evt <- eventsFired
      if(eventsList.contains(evt) && !gath.contains(evt))
    } yield evt

    if(gathered.length == eventsCount){
      sendResponseAndShutdown(HandlerResponse)
    } else {
      //TODO: handle else case
    }*/
    /*resultsFired match {
      case (Some(a), Some(b)) =>
        println("Results received for both events")
        //timeoutMessage.cancel
        sendResponseAndShutdown(resultsFired)
      case _ =>
        println("Results not ready yet")
    }*/
  }

  private def getActor(underlying: AnyRef): ActorRef = {
    def getter(name: String) = { actors.get(name) match { case Some(actor) => actor } }
    underlying match {
      case Sell(s, p, a) => getter("seller")
      case Buy(s, p, a)  => getter("buyer")
      case Price(s, p)   => getter("pricer")
    }
  }

  private def sendResponseAndShutdown(response: Any) = {
    originalSender ! response
    // TODO: no need to shutdown as other events are coming in to this actor
    //println("Stopping context capturing actor")
    //context.stop(self)
  }

  //import context.dispatcher
  //val timeoutMessage = context.system.scheduler.scheduleOnce(delay){self ! RequestTimeout}
}