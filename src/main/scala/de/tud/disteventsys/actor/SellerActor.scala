package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import akka.event.Logging
import de.tud.disteventsys.event.Event._

/**
  * Created by ms on 16.01.17.
  */
// actor that gets forwarded Sell Events
class SellerActor extends Actor with ActorLogging{
  override val log = Logging(context.system, this)
   def receive: Receive = {

    case EsperEvent(className, underlying) =>
      println(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case Sell(s, p, a) =>
          log.info(s"Received Sell: ${s}, ${p}, ${a}")
        case _ => log.info("SellerAcrot: Something else")
      }
    case _ => log.info(s"SellerActor: Could not find a corresponding case class")
  }
}
