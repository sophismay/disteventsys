package de.tud.disteventsys.actor

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, ReceiveTimeout}
import com.espertech.esper.client.EPStatement
import de.tud.disteventsys.esper.EsperEngine

import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by ms on 18.11.16.
  */

object EsperActor{
  // to start processing events by esper engine
  case object StartProcessing

  // register actor class with esper engine
  case class RegisterEventType(name: String, clz: Class[_ <: Any])

  // considering case without listener too
  //case class DeployStatement(epl: String, listener: Option[ActorRef])
  case class DeployStatement(eplStatement: String, name: String)

  case class CreateActor(clz: String)

  case class ReceiveCreatedActor(actor: ActorRef)

  case class DeployStatements(eplStatement: String)

  case object UnregisterAllEvents

  //case class Deploy(eplStatement: String, name: String)
}


class EsperActor extends Actor with ActorLogging with EsperEngine{
  import EsperActor._
  // service unavailable if nothing processed within 20 seconds
  context.setReceiveTimeout(20 seconds)

  private var createdActors: List[ActorRef] = List.empty
  private var currentEsperStatement: EPStatement = _

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute){
      case _: ArithmeticException       => Resume
      case _: NullPointerException      => Restart
      case _: IllegalArgumentException  => Stop
      case _: Exception                 => Escalate
    }

  def receive: Receive = {

    case ReceiveTimeout =>
      // no progress within 20 seconds, shutting down
      log.error("Shutting down due to unavailable service")
      context.system.terminate()
    //
    case RegisterEventType(name, clz)   =>
      esperConfig.addEventType(name, clz.getName)

    /*case DeployStatement(epl, listener) =>
      println(s"INSIDE DEPLOY STATEMENT: ${listener}")
      createEPL(epl)(evt => listener map ( l => l ! evt))*/

    case StartProcessing                =>
      context.become(dispatchingToEsper)

    case CreateActor(clz)       =>
      clz match {
        case "Buy"  =>
          val actor = context.actorOf(Props(classOf[BuyerActor]), "buyer")
          createdActors = createdActors :+ actor
          println(s"ACTOR CREATED: ${actor}")
        case "Sell" =>
          createdActors = createdActors :+ context.actorOf(Props(classOf[SellerActor]), "seller")
        case "Price" =>
          createdActors = createdActors :+ context.actorOf(Props(classOf[PriceActor]), "price")
      }
    case DeployStatement(eplStatement, name) =>
      println(s"CASE DEPLOY: $name")
      val actors = for {
        actor <- createdActors
        if(actor.path.name == name)
      } yield actor
      // making EsperActor the sender
      val handler = context.actorOf(NotifierActor.props(self, 1 second), "notifier")
      // now match events and tell them something
      val actor = actors.head
      val esperStatement: Try[EPStatement] = createEPL(eplStatement){
        evt =>
          actor.tell(evt, handler)
      }
      println(s"ESPER STATEMENT TRY: $esperStatement")
      /*val actor = {for {
        actor <- createdActors
        if(actor.path.name == name)
      } yield actor}.head
      println(s"CASE DEPLOY: $actor")
      createEPL(eplStatement)(evt => actor ! evt)*/

    case DeployStatements(eplStatement: String) =>
      log.debug("CASE DEPLOY STATEMENT TO ALL INVOLVED ACTORS")
      val handler = context.actorOf(NotifierActor.props(self, 1 second), "notifier")
      //createdActors foreach { actor => createEPL(eplStatement)(evt => actor.tell(evt, handler))}
      //createEPL(eplStatement)(evt => createdActors.head.tell(evt, handler))
      //createEPL(eplStatement)(evt => createdActors(1).tell(evt, handler))
      val esperStatement: Try[EPStatement] = createEPL(eplStatement){
        evt =>
          createdActors map { actor =>
            actor.tell(evt, handler) }
      }
      currentEsperStatement = esperStatement.getOrElse( throw new NoSuchFieldError )
      println(s"ESPER STATEMENT TRY: $currentEsperStatement")

    case UnregisterAllEvents =>
      if(!currentEsperStatement.isStopped) currentEsperStatement.removeAllListeners()

  }

  private def dispatchingToEsper(): Receive = {
    case evt@_ => epRuntime.sendEvent(evt)
  }
}
