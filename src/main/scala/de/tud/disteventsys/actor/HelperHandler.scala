package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.Logging
import de.tud.disteventsys.actor.HelperHandler.Messages._
import de.tud.disteventsys.actor.MainHandler.Messages.SecondHandlerResponse
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}
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

class HelperHandler(originalSender: ActorRef, actors: Map[String, ActorRef], event: AnyRef) extends Actor with ActorLogging{
  override val log = Logging(context.system, this)
  override def preStart = {
    super.preStart()
    log.debug("STARTING HELPER HANDLER")
  }
  def receive =  {
    case EsperEvent(clz, underlying) =>
      val actor = getActor(underlying)
      underlying match {
        case evt@_ =>
          if(evt.getClass == event.getClass){
            actor ! EsperEvent(clz, underlying)
            originalSender ! SecondHandlerResponse
          }
      }
    case StartOperation(id, evt) =>
      log.info(s"HELPER HANDLER: Received Start Operation Message, $id $evt")
      doOperation(id, evt, sender())
  }

  private def getActor(underlying: AnyRef): ActorRef = {
    def getter(name: String) = { actors.get(name) match { case Some(actor) => actor } }
    underlying match {
      case Sell(s, p, a) => getter("seller")
      case Buy(s, p, a)  => getter("buyer")
      case Price(s, p)   => getter("pricer")
    }
  }

  private def doOperation(id: Long, evt: AnyRef, sender: ActorRef) = {
    // TODO: do some time Consuming task, maybe not
    import context.dispatcher
    val timeout = context.system.scheduler.scheduleOnce(80 millis){
      sender ! HelperResponse(id, None)
    }
    // timeout.cancel
  }
}