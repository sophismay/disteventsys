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

  def props(originalSender: ActorRef, actors: Map[String, ActorRef], delay: FiniteDuration): Props = {
    Props(new MainHandler(originalSender, actors, delay))
  }
}

class MainHandler(originalSender: ActorRef, actors: Map[String, ActorRef], delay: FiniteDuration) extends Actor with ActorLogging {
  import MainHandler.Messages._

  private final val eventsCount = 2
  private var resultsFired = Tuple2[Option[Any], Option[Any]] (Some(1), Some(2))

  def receive = LoggingReceive {
    case EsperEvent(clz, underlying) =>
      println(s"ESPER EVENT IN HANDLEr: $clz $underlying")
      underlying match {
        case Buy(s, p, a) =>
          actors.get("buyer") match {
            case Some(act) =>
              act ! Buy(s, p, a)
            case _         => println("BUY NOT MATCHED IN HANDLER")
          }
        case Price(s, p) =>
          actors.get("pricer") match {
            case Some(act) =>
              act ! Price(s, p)
            case _         => println("PRICE NOT MATCHED IN HANDLER")
          }
        case Sell(s, p, a)  =>
          actors.get("seller") match {
            case Some(act) =>
              act ! Sell(s, p, a)
            case _         => println("SELL NOT MATCHED IN HANDLER")
          }
        //actors.default("buyer") ! Buy(s, p, a)
      }
    case HandlerResponse =>
      // add response to resultsFired, then collectResponse
      collectResponse
    case RequestTimeout  =>
      println("TIMEOUT RECEIVED: ")
      sendResponseAndShutdown(RequestTimeout)
  }

  private def collectResponse = {
    resultsFired match {
      case (Some(a), Some(b)) =>
        println("Results received for both events")
        timeoutMessage.cancel
        sendResponseAndShutdown(resultsFired)
      case _ =>
        println("Results not ready yet")
    }
  }

  private def sendResponseAndShutdown(response: Any) = {
    originalSender ! response
    println("Stopping context capturing actor")
    context.stop(self)
  }

  import context.dispatcher
  val timeoutMessage = context.system.scheduler.scheduleOnce(delay){self ! RequestTimeout}
}