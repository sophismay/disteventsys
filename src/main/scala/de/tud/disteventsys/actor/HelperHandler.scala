package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import de.tud.disteventsys.actor.MainHandler.Messages.SecondHandlerResponse
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}

/**
  * Created by ms on 07.02.17.
  */
object HelpingHandler {

}

class SecondHandler(originalSender: ActorRef, actors: Map[String, ActorRef], event: AnyRef) extends Actor with ActorLogging{
  def receive = LoggingReceive {
    case EsperEvent(clz, underlying) =>
      val actor = getActor(underlying)
      underlying match {
        case evt@_ =>
          if(evt.getClass == event.getClass){
            actor ! EsperEvent(clz, underlying)
            originalSender ! SecondHandlerResponse
          }
      }
  }

  private def getActor(underlying: AnyRef): ActorRef = {
    def getter(name: String) = { actors.get(name) match { case Some(actor) => actor } }
    underlying match {
      case Sell(s, p, a) => getter("seller")
      case Buy(s, p, a)  => getter("buyer")
      case Price(s, p)   => getter("pricer")
    }
  }
}