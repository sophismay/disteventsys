package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import akka.event.Logging
import de.tud.disteventsys.event.Event._

/**
  * Created by ms on 16.01.17.
  */
// Actor that gets forwarded fired Price Events
class PriceActor extends Actor with ActorLogging{
  override val log = Logging(context.system, this)
  override def receive: Receive = {

    case EsperEvent(className, underlying) =>
      log.info(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case Price(s, p) =>
          log.info(s"Received Price Event: ${s}, ${p}")
      }
    case _ => println(s"Could not find a corresponding case class")
  }
}
