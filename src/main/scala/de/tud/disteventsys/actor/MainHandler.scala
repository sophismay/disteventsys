package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}

import scala.concurrent.duration.FiniteDuration

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
            hHandler: Option[ActorRef]): Props = {
    Props(new MainHandler(originalSender, actors, eventsList, hHandler))
  }
}

class MainHandler(originalSender: ActorRef, actors: Map[String, ActorRef], eventsList: List[String],
                  hHandler: Option[ActorRef]) extends Actor with ActorLogging {
  import MainHandler.Messages._

  private final val eventsCount = eventsList.length
  private var eventsFired: Array[_] = Array.empty

  def receive = LoggingReceive {
    case EsperEvent(clz, underlying) =>
      val actor = getActor(underlying)
      println(s"ESPER EVENT IN HANDLEr: $clz $underlying")
      underlying match {
        case Buy(s, p, a)  =>
          actor ! Buy(s, p, a)
          handleFiredEvent(Buy(s, p, a))
        case Price(s, p)   =>
          actor ! Price(s, p)
          handleFiredEvent(Price(s, p))
        case Sell(s, p, a) =>
          actor ! Sell(s, p, a)
          handleFiredEvent(Sell(s, p, a))
      }
    case HandlerResponse =>
      // add response to resultsFired, then collectResponse
      collectResponse
    case RequestTimeout  =>
      println("TIMEOUT RECEIVED: ")
      sendResponseAndShutdown(RequestTimeout)
  }

  private def handleFiredEvent(evt: AnyRef) = {
    eventsFired = eventsFired :+ evt
    // collect response
    collectResponse
  }

  private def collectResponse = {
    var gath: Array[String] = Array.empty
    val gathered = for {
      evt <- eventsFired
      if(eventsList.contains(evt) && !gath.contains(evt))
    } yield evt

    if(gathered.length == eventsCount){
      sendResponseAndShutdown(HandlerResponse)
    } else {
      //TODO: handle else case
    }
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