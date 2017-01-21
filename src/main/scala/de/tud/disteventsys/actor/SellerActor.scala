package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import akka.actor.Actor.Receive
import de.tud.disteventsys.actor_classes.Buy
import de.tud.disteventsys.event.EsperEvent

/**
  * Created by ms on 16.01.17.
  */
class SellerActor extends Actor with ActorLogging{
  override def receive: Receive = {

    case EsperEvent(className, underlying) =>
      println(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case _ => println("NOt Handling yet")
        /*case Sell(s, p, a) =>
          println(s"Received Sell: ${s}, ${p}, ${a}")*/
      }
    case _ => println(s"Could not find a corresponding case class")
  }
}