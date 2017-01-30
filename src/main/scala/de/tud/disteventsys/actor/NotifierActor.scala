package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive

import scala.concurrent.duration
import scala.concurrent.duration.{FiniteDuration, TimeUnit}

/**
  * Created by ms on 30.01.17.
  */

object NotifierActor{
  case object RequestTimeout

  def props(actors: List[Option[ActorRef]], originalSender: ActorRef, delay: FiniteDuration): Props = {
    Props(new NotifierActor(actors, originalSender, delay))
  }
}

class NotifierActor(actors: List[Option[ActorRef]], originalSender: ActorRef, delay: FiniteDuration) extends Actor with ActorLogging{

  import NotifierActor._

  def receive = LoggingReceive {
    case RequestTimeout => log.info(s"TIMEOUT RECEIVED: ")
  }

  import context.dispatcher
  val timeoutMessage = context.system.scheduler.scheduleOnce(delay){self ! RequestTimeout}
}
