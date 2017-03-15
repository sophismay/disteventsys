package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import de.tud.disteventsys.event.Event.{ Buy, EsperEvent }

/**
  * Created by ms on 23.11.16.
  */
// Actor that gets forwarded fired Buy Events
class BuyerActor extends Actor with ActorLogging {
  def receive: Receive = {

    case EsperEvent(className, underlying) =>
      println(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case Buy(s, p, a) =>
          println(s"Received Buy: ${s}, ${p}, ${a}")
      }
    case Buy(s, p, a) =>
      println(s"Received Buy: ${s}, ${p}, ${a}")
    case _ => println(s"Could not find a matching message")
  }
}
