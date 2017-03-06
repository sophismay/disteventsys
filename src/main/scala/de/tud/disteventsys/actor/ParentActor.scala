package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.event.Logging
import de.tud.disteventsys.actor.EsperActor.{DeployStatement, RegisterEventType, StartProcessing}
import de.tud.disteventsys.actor.ParentActor.{CreateStatement, DispatchedEsperEvent}
import de.tud.disteventsys.dsl.Tree
import de.tud.disteventsys.esper.{EsperEngine, Statement}
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}
import de.tud.disteventsys.esper.Stream
import jdk.management.resource.internal.inst.DatagramDispatcherRMHooks

import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by ms on 06.03.17.
  */
object ParentActor {
  sealed abstract class ParentMessage
  case class CreateStatement[T](stringBuilder: StringBuilder, node: Tree[T], flag: Boolean) extends ParentMessage
  // sent to esperactor to inform of other child actors
  case class AddedActor(actor: ActorRef)                                                    extends ParentMessage
  case class DispatchedEsperEvent(evt: EsperEvent)

  def props = Props(new ParentActor)
}

class ParentActor extends Actor with ActorLogging {
  //var createdActors: Array[ActorRef] = Array.empty
  // map of actornames to Tuple2(ActorRef, statement)
  var createdActors: Map[String, Tuple2[ActorRef, Stream[_]]] = Map.empty
  val rand = new Random()
  private lazy val buyerActor = context.actorOf(Props(classOf[BuyerActor]), "buyer")
  private lazy val priceActor = context.actorOf(Props(classOf[PriceActor]), "pricer")
  private lazy val sellerActor = context.actorOf(Props(classOf[SellerActor]), "seller")
  private val stockActors: Map[String, ActorRef] = Map(
    buyerActor.path.name  -> buyerActor,
    priceActor.path.name  -> priceActor,
    sellerActor.path.name -> sellerActor
  )
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
      // handling second and more calls
      createAndDeployActor(sb, node)
    case DispatchedEsperEvent(evt) =>
      log.info(s"Dispatched Esper Event called: $evt")
      evt match {
        case EsperEvent(clz, underlying) =>
          val actor = getActor(underlying)
          actor ! underlying
      }
  }

  private def createAndDeployActor(sb: StringBuilder, node: Tree[_]) = {
    val esperActorName = s"esperactor-${rand.nextLong()}"
    val esperActor = context.actorOf(EsperActor.props(self), esperActorName)
    val statement = new Statement()
    statement.initEpl(sb)
    val eplStatement = statement.getEplStatement
    createdActors = createdActors + (esperActorName -> Tuple2(esperActor, new Stream(statement, node)))
    println(s"CReated ACtors: ${createdActors.size} $createdActors")
    // register events
    statement.getAllEvents foreach {
      case (clz, underlyingClass) =>
        esperActor ! RegisterEventType(clz, underlyingClass)
    }
    esperActor ! DeployStatement(eplStatement)
    esperActor ! StartProcessing

    dummyData(esperActor)
  }

  private def checkEventsAndSendToRightActors = {

  }

  private def dummyData(esperActor: ActorRef) = {
    val prices = Array(
      Price("BP", 7.61), Price("RDSA", 2101.00), Price("RDSA", 2209.00),
      Price("BP",7.66), Price("BP", 7.64), Price("BP", 7.67)
    )

    prices foreach (esperActor ! _)
  }

  private def getActor(underlying: AnyRef): ActorRef = {
    def getter(name: String) = { stockActors.get(name) match { case Some(actor) => actor } }
    underlying match {
      case Sell(s, p, a) => getter("seller")
      case Buy(s, p, a)  => getter("buyer")
      case Price(s, p)   => getter("pricer")
    }
  }
}
