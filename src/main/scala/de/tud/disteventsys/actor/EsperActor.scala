package de.tud.disteventsys.actor

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, ReceiveTimeout}
import akka.event.LoggingReceive
import com.espertech.esper.client.EPStatement
import de.tud.disteventsys.actor.MainHandler.Messages.SecondHandlerResponse
import de.tud.disteventsys.esper.EsperEngine
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}

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

  case class InitializeActors(actors: Map[String, ActorRef])

  // considering case without listener too
  //case class DeployStatement(epl: String, listener: Option[ActorRef])
  case class DeployStatement(eplStatement: String, name: String)

  case class CreateActor(clz: String)

  case class ReceiveCreatedActor(actor: ActorRef)

  case class DeployStatements(eplStatement: String)

  case class DeployStatementss(statements: Array[String])

  case object UnregisterAllEvents

  case class DeployStream(eventWithFields: Tuple2[String, List[String]])

  case class DeployStatementsss(eplStatements: Array[String])

  //case class Deploy(eplStatement: String, name: String)
}


class EsperActor extends Actor with ActorLogging with EsperEngine{
  import EsperActor._
  import scala.util.Random
  val rand = new Random()
  // service unavailable if nothing processed within 20 seconds
  context.setReceiveTimeout(20 seconds)

  private var createdActors: List[ActorRef] = List.empty
  private var currentEsperStatement: EPStatement = _
  private var handlers: Array[ActorRef] = Array.empty
  private var actors: Map[String, ActorRef] = Map.empty
  private var EPStatements: Array[Try[EPStatement]] = Array.empty
  private var EPStatement: Try[EPStatement] = _

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute){
      case _: ArithmeticException       => Resume
      case _: NullPointerException      => Restart
      case _: IllegalArgumentException  => Stop
      case _: Exception                 => Escalate
    }

  def receive: Receive = beforeDispatching()

  private def beforeDispatching(): Receive = {
    case ReceiveTimeout =>
      // no progress within 20 seconds, shutting down
      log.error("Shutting down due to unavailable service")
      context.system.terminate()
    //
    case RegisterEventType(name, clz)   =>
      println(s"REGISTERING EVENT: ${clz.getName} $name")
      println(s"EVENT TYPES ${esperConfig.getEventTypeNames.keySet()} ")

      val eventTypes = esperConfig.getEventTypeNames.keySet()
      if(!eventTypes.contains(name)){
        println(s"EVENT NOT YET REGISTERED, now registering: $name")
        esperConfig.addEventType(name, clz.getName)
      }
    //TODO: is this needed? just create in ActorCreator and leave as it is?
    case InitializeActors(mActors: Map[String, ActorRef]) =>
      if(actors.keys.size == 0){
        actors = actors ++ mActors
        println(s"ACTIRS: $actors")
      }

    case DeployStream(eventWithFields: Tuple2[String, List[String]]) =>

      //val handler = context.actorOf(Handler.props(self, actors, 10 seconds), s"handler${rand.nextLong()}")
      //handlers = handlers :+ handler


    /*case DeployStatement(epl, listener) =>
      println(s"INSIDE DEPLOY STATEMENT: ${listener}")
      createEPL(epl)(evt => listener map ( l => l ! evt))*/

    case StartProcessing                =>
      context.become(dispatchingToEsper)

    case DeployStatementsss(eplStatements: Array[String]) =>
      val handler = context.actorOf(MainHandler.props(self, actors, 10 seconds), s"handler${rand.nextLong()}")
      eplStatements foreach {
        es =>
          EPStatement = createEPL(es){evt => handler ! evt }
      }

    /*case CreateActor(clz)       =>
      clz match {
        case "Buy"  =>
          val actor = context.actorOf(Props(classOf[BuyerActor]), "buyer")
          createdActors = createdActors :+ actor
          println(s"ACTOR CREATED: ${actor}")
        case "Sell" =>
          createdActors = createdActors :+ context.actorOf(Props(classOf[SellerActor]), "seller")
        case "Price" =>
          createdActors = createdActors :+ context.actorOf(Props(classOf[PriceActor]), "price")
      }*/
    case CreateActor =>
      println("CREATE ACTOR CALLED------------")
    case DeployStatementss(statements: Array[String]) =>
      //val handler = context.actorOf(Handler.props(self, 1 second), "handler")
      statements foreach {
        statement =>
          val handler = context.actorOf(MainHandler.props(self, actors, 10 seconds), "handler" + rand.nextLong())
          handlers = handlers :+ handler
          EPStatements = EPStatements :+
            createEPL(statement){evt => handler ! evt;println(s"HANDLER CREATED $handler for statement $statement with event $evt")}
      }

    case DeployStatement(eplStatement, name) =>
      println(s"CASE DEPLOY: $name")
      val actors = for {
        actor <- createdActors
        if(actor.path.name == name)
      } yield actor
      // making EsperActor the sender
      val handler = context.actorOf(NotifierActor.props(self, 10 second), "notifier")
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
      println("CASE DEPLOY STATEMENT TO ALL INVOLVED ACTORS")
      val handler = context.actorOf(NotifierActor.props(self, 10 seconds), "notifier")
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
      EPStatements foreach {
        tryStat =>
          //val stat = tryStat.getOrElse(throw new NoSuchElementException)
          val stat = tryStat.get
          println(s"STAT FOUND, UNREGISTERING: ${stat}")
          stat.destroy()
      }
      // reset
      EPStatements = Array.empty
      handlers = Array.empty
      //println(s"BEFORE UNREGISTERING: ${currentEsperStatement.getUpdateListeners}")
    /*if(!currentEsperStatement.isStopped)
      currentEsperStatement.removeAllListeners()*/

  }

  private def dispatchingToEsper(): Receive = {
    case UnregisterAllEvents =>
      println(s"CASE UNREGISTER ALL EVTS: ${currentEsperStatement}")
      /*if(!currentEsperStatement.isStopped)
        currentEsperStatement.removeAllListeners()*/
      //println(s"AFTER UNREGISTERING, removing listeners: ${currentEsperStatement.getUpdateListeners}")
      context.become(beforeDispatching)
      self ! UnregisterAllEvents
    // TODO: evt@_ somewhat generic and hence Received Buy is logged 12 times when it's above case UnregisterAllEvents
    case evt@_ =>
      println(s"CASE EVT DISPATCH: ${evt}")
      epRuntime.sendEvent(evt)
    case _ =>
  }
}


