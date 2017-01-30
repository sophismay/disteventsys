package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import de.tud.disteventsys.event.{EsperEvent, Price}

/**
  * Created by ms on 16.01.17.
  */
class PriceActor extends Actor with ActorLogging{
  override def receive: Receive = {

    case EsperEvent(className, underlying) =>
      println(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case Price(s, p) =>
          println(s"Received Price Event: ${s}, ${p}")
      }
    case _ => println(s"Could not find a corresponding case class")
  }
}
