package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.event.Logging
import de.tud.disteventsys.actor.EsperActor.{DeployStatement, RegisterEventType, StartProcessing}
import de.tud.disteventsys.actor.ParentActor.CreateStatement
import de.tud.disteventsys.dsl.Tree
import de.tud.disteventsys.esper.{EsperEngine, Statement}
import de.tud.disteventsys.event.Event.Price

import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by ms on 06.03.17.
  */
object ParentActor {
  sealed abstract class ParentMessage
  case class CreateStatement(stringBuilder: StringBuilder, node: Tree[T], flag: Boolean) extends ParentMessage
  // sent to esperactor to inform of other child actors
  case class AddedActor(actor: ActorRef)                                                 extends ParentMessage

  def props = Props(new ParentActor)
}

class ParentActor extends Actor with ActorLogging {
  var createdActors: Array[ActorRef] = Array.empty
  val rand = new Random()
  override val log = Logging(context.system, this)
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute){
    case _: ArithmeticException       => Resume
    case _: NullPointerException      => Restart
    case _: IllegalArgumentException  => Stop
    case _: Exception                 => Escalate
  }

  override def preStart = {
    super.preStart()
    log.debug("STARTING HELPER HANDLER")
  }

  def receive = {
    case CreateStatement(sb, node, flag) =>
      log.info("CREATe STATement called")
      val esperActor = context.actorOf(EsperActor.props(self), s"esperactor-${rand.nextLong()}")
      createdActors = createdActors :+ esperActor
      val statement = new Statement()
      statement.initEpl(sb)
      val eplStatement = statement.getEplStatement
      // register events
      statement.getAllEvents foreach {
        case (clz, underlyingClass) =>
          esperActor ! RegisterEventType(clz, underlyingClass)
      }
      esperActor ! DeployStatement(eplStatement)
      esperActor ! StartProcessing
  }

  private def dummyData = {
    val prices = Array(
      Price("BP", 7.61), Price("RDSA", 2101.00), Price("RDSA", 2209.00),
      Price("BP",7.66), Price("BP", 7.64), Price("BP", 7.67)
    )

    //prices foreach (esperActor ! _)
  }
}
