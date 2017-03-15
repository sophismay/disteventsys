package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
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
    case class StopTimeout(id: Long)                          extends ScheduleMessage
  }

  def props: Props = Props(new ScheduleActor)
}

/*
  Actor that sets timer and sends messages if time reached
 */
class ScheduleActor extends Actor with ActorLogging{
  import ScheduleActor.Messages._

  override val log = Logging(context.system, this)
  // to keep track of ids for which timeouts are not to be sent
  // because they have been fulfilled by response from helper handler
  private var timeoutsStopped: List[Long] = List.empty
  private var timeouts: Map[Long, Cancellable] = Map.empty

  override def preStart(): Unit = {
    super.preStart()
    log.debug("STARTING SCHEDULE ACTOR")
  }

  def receive =  {
    case StartTimer(id, duration) =>
      log.info(s"RECEIVED START TIMER WITH DURATION $duration $id")
      startTimer(id, duration, sender())
    case StopTimeout(id) =>
      log.info(s"Scheduler Actor: Received Stop Timeout for $id")
      timeoutsStopped = timeoutsStopped :+ id
      val timeout = (timeouts find { case (ind, to) => ind == id }).get._2
      timeout.cancel
      log.info(s"Scheduler Actor: Stopped timeout for $id")
  }

  private def startTimer(id: Long, duration: FiniteDuration, sender: ActorRef) = {
    import context.dispatcher
    val timeout = context.system.scheduler.scheduleOnce(duration){
      if(!timeoutsStopped.contains(id)) {
        sender ! Timeout(id)
      }
    }
    timeouts = timeouts + (id -> timeout)
  }
}
