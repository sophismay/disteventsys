package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import de.tud.disteventsys.event.{Buy, EsperEvent}

/**
  * Created by ms on 23.11.16.
  */
class BuyerActor extends Actor with ActorLogging {
  override def receive: Receive = {

    case EsperEvent(className, underlying) =>
      println(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case Buy(s, p, a) =>
          println(s"Received Buy: ${s}, ${p}, ${a}")
      }
    case _ => println(s"Could not find a corresponding case class")
  }
}
