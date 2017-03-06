package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging}
import de.tud.disteventsys.event.Event._

/**
  * Created by ms on 16.01.17.
  */
class PriceActor extends Actor with ActorLogging{
  override def receive: Receive = {

    //TODO: Buy events get here but question is how come...
    //TODO: would be ideal if events go to respective actors
    case Price(s, p) =>
      println(s"Received Price: ${s}, ${p}")
    case _ => println(s"Could not find a corresponding case class")
  }
}
