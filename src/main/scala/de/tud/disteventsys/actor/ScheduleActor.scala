package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive

import scala.concurrent.duration.FiniteDuration
import akka.event.Logging

/**
  * Created by ms on 01.03.17.
  */
object ScheduleActor {
  object Messages {
    sealed abstract class ScheduleMessage
    case class StartTimer(id: Long, duration: FiniteDuration) extends ScheduleMessage
    case class Timeout(id: Long)                              extends ScheduleMessage
  }

  def props: Props = Props(new ScheduleActor)
}

class ScheduleActor extends Actor with ActorLogging{
  import ScheduleActor.Messages._

  override val log = Logging(context.system, this)

  override def preStart(): Unit = {
    super.preStart()
    log.debug("STARTING SCHEDULE ACTOR")
  }

  def receive =  {
    case StartTimer(id, duration) =>
      log.info(s"RECEIVED START TIMER WITH DURATION $duration $id")
      startTimer(id, duration, sender())
  }

  private def startTimer(id: Long, duration: FiniteDuration, sender: ActorRef) = {
    val timeout = context.system.scheduler.scheduleOnce(duration){
      sender ! Timeout(id)
    }
    // timeout.cancel
  }
}
