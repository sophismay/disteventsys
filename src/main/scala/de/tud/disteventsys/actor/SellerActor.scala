package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import akka.actor.Actor.Receive
import de.tud.disteventsys.event.Event._

/**
  * Created by ms on 16.01.17.
  */
class SellerActor extends Actor with ActorLogging{
   def receive: Receive = {
     case Sell(s, p, a) =>
       println(s"Received Sell: $s, $p, $a")
     case _ => println(s"Could not find a corresponding case class")
  }
}
