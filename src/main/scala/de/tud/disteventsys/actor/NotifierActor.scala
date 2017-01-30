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

  def props(originalSender: ActorRef, delay: FiniteDuration): Props = {
    Props(new NotifierActor(originalSender, delay))
  }
}

class NotifierActor(originalSender: ActorRef, delay: FiniteDuration) extends Actor with ActorLogging{

  import NotifierActor._

  def receive = LoggingReceive {
    case RequestTimeout =>
      log.debug(s"TIMEOUT RECEIVED: ")
      sendResponseAndShutdown(RequestTimeout)
  }

  private def sendResponseAndShutdown(response: Any) = {
    originalSender ! response
    log.debug(s"Stopping context capturing actor")
    context.stop(self)
  }

  import context.dispatcher
  val timeoutMessage = context.system.scheduler.scheduleOnce(delay){self ! RequestTimeout}
}
