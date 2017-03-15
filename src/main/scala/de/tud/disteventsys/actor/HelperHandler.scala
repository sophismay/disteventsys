package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.Logging
import de.tud.disteventsys.actor.HelperHandler.Messages._
import scala.concurrent.duration._

/**
  * Created by ms on 07.02.17.
  */
object HelperHandler {
  object Messages {
    sealed abstract class HelperMessage
    case class StartOperation(id: Long, evt: AnyRef)      extends HelperMessage
    case class HelperResponse(id: Long, response: AnyRef) extends HelperMessage
  }
  def props(originalSender: ActorRef, actors: Map[String, ActorRef], event: AnyRef): Props = {
    Props(new HelperHandler(originalSender, actors, event))
  }
}

// Handle the situation when a stream depends on another
// under time conditions
class HelperHandler(originalSender: ActorRef, actors: Map[String, ActorRef], event: AnyRef) extends Actor with ActorLogging{
  override val log = Logging(context.system, this)
  override def preStart = {
    super.preStart()
    log.debug("STARTING HELPER HANDLER")
  }
  def receive =  {
    case StartOperation(id, evt) =>
      log.info(s"HELPER HANDLER: Received Start Operation Message, $id $evt")
      doOperation(id, evt, sender())
  }

  private def doOperation(id: Long, evt: AnyRef, sender: ActorRef) = {
    import context.dispatcher
    // might be a more time consuming task
    // current time after which event fired from helper handler is considered delayed is set to 100 millis
    val timeout = context.system.scheduler.scheduleOnce(80 millis){
      sender ! HelperResponse(id, None)
    }
    // timeout.cancel
  }
}