package de.tud.disteventsys.actor

import akka.actor.Actor.Receive
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, ReceiveTimeout}
import akka.event.LoggingReceive
import com.espertech.esper.client.EPStatement
import de.tud.disteventsys.actor.EsperActor.RegisterEventType
import de.tud.disteventsys.esper.{EsperEngine, Statement}
import de.tud.disteventsys.event.Event.{Buy, EsperEvent, Price, Sell}
import de.tud.disteventsys.esper.Engine

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

  //case class InitializeActors(actors: Map[String, ActorRef])

  // considering case without listener too
  //case class DeployStatement(epl: String, listener: Option[ActorRef])
  case class DeployStatement(statement: String)

  //case class CreateActor(clz: String)

  //case class ReceiveCreatedActor(actor: ActorRef)

  //case class DeployStatements(eplStatement: String)

  //case class DeployStatementss(statements: Array[String])

  //case object UnregisterAllEvents

  //case class DeployStream(eventWithFields: Tuple2[String, List[String]])

  //case class DeployStatementsss(eplStatements: Array[String], eventsList: List[String],
  //                              eventWithFields: Option[Tuple2[String, List[String]]], actionEvent: String)

  def props(originalSender: ActorRef): Props = {
    Props(new EsperActor(originalSender))
  }

}


class EsperActor(originalSender: ActorRef) extends Actor with ActorLogging {
  import EsperActor._
  import scala.util.Random
  val esperEngine = new Engine()
  val esperConfig = esperEngine.getConfig
  val rand = new Random()
  val eventTimeoutDuration: FiniteDuration = 100 millis
  // service unavailable if nothing processed within 20 seconds
  //context.setReceiveTimeout(20 seconds)

  private var createdActors: List[ActorRef] = List.empty
  private var currentEsperStatement: EPStatement = _
  private var handlers: Array[ActorRef] = Array.empty
  //private var actors: Map[String, ActorRef] = Map.empty
  private var EPStatements: Array[Try[EPStatement]] = Array.empty
  private var EPStatement: Try[EPStatement] = _
  private val delay = 10 seconds
  private var currentMainHandler: ActorRef = _

  def receive: Receive = beforeDispatching()

  private def beforeDispatching(): Receive = {
    case ReceiveTimeout =>
      // no progress within 20 seconds, shutting down
      log.error("Shutting down due to unavailable service")
      context.system.terminate()
    //
    case RegisterEventType(name, clz)   =>
      println(s"REGISTERING EVENT: ${clz.getName} $name")
      //println(s"EVENT TYPES ${esperConfig.getEventTypeNames.keySet()} ")

      val eventTypes = esperConfig.getEventTypeNames.keySet()
      if(!eventTypes.contains(name)){
        println(s"EVENT NOT YET REGISTERED, now registering: $name")
        esperConfig.addEventType(name, clz.getName)
      }
      //println(s"lASt stage OF ReGISTEREVENTTYPE $esperConfig")

    //TODO: is this needed? just create in ActorCreator and leave as it is?
    /*case InitializeActors(mActors: Map[String, ActorRef]) =>
      if(actors.keys.size == 0){
        actors = actors ++ mActors
        println(s"ACTIRS: $actors")
      }*/

    case DeployStatement(eplStatement: String) =>

    case StartProcessing                =>
      context.become(dispatchingToEsper)

    /*case DeployStatementsss(eplStatements, eventsList, eventWithFields, actionEvent) =>
      // TODO: helper handler passing condition
      // TODO: event passed to helperHandler
      println(s"DEPLOY CALLED WITH: $eventWithFields")
      eventWithFields match {
        case Some(evtwf) =>
          killChildActor
          val helperHandler = context.actorOf(HelperHandler.props(self, actors, evtwf))
          currentMainHandler = context.actorOf(MainHandler.props(self, actors, eventsList,
            Some(helperHandler), actionEvent, eventTimeoutDuration), s"handler${rand.nextLong()}")
          println(s"MATCH IN SOME: , calling execute statements")
          //eplStatements foreach { s => println(s"Before Executing Statement in Some: $s")}
          executeStatements(eplStatements, currentMainHandler)
        case None =>
          currentMainHandler = context.actorOf(MainHandler.props(self, actors, eventsList,
            None, actionEvent, eventTimeoutDuration), s"handler${rand.nextLong()}")
          println("NO MATCH in deploy")
          //eplStatements foreach { s => println(s"Before Executing Statement in None: $s")}
          executeStatements(eplStatements, currentMainHandler)
      }*/

    /*case CreateActor =>
      println("CREATE ACTOR CALLED------------")*/

    /*case UnregisterAllEvents =>
      resetEPStatements*/
      //epService.removeAllStatementStateListeners()
      //epService.removeAllServiceStateListeners()
      //epService.getEPAdministrator.stopAllStatements
      //println(s"BEFORE UNREGISTERING: ${currentEsperStatement.getUpdateListeners}")
    /*if(!currentEsperStatement.isStopped)
      currentEsperStatement.removeAllListeners()*/

    /*case evt@_ =>
      println(s"CASE EVT DISPATCH: ${evt}")
      epRuntime.sendEvent(evt)*/

  }

  private def executeStatements(eplStatements: Array[String], mainHandler: ActorRef) = {
    eplStatements foreach {
      es =>
        EPStatement = esperEngine.createEPL(es){evt => mainHandler ! evt }
        println(s"EPStatement created: $EPStatement")
        // below needed?
        EPStatements = EPStatements :+ EPStatement
    }

  }

  private def killChildActor = {
    context.stop(currentMainHandler)
  }

  private def resetEPStatements = {
    EPStatements foreach {
      tryStat =>
        //val stat = tryStat.getOrElse(throw new NoSuchElementException)
        val stat = tryStat.get
        println(s"STAT FOUND, UNREGISTERING: ${stat} $EPStatement")
        stat.destroy()
    }
    // reset
    //EPStatement.map( s => s.destroy())
    EPStatements = Array.empty
    handlers = Array.empty
  }

  private def dispatchingToEsper(): Receive = {
    case UnregisterAllEvents =>
      println(s"CASE UNREGISTER ALL EVTS: ${EPStatement}")

      /*if(!currentEsperStatement.isStopped)
        currentEsperStatement.removeAllListeners()*/
      //println(s"AFTER UNREGISTERING, removing listeners: ${currentEsperStatement.getUpdateListeners}")
      context.become(beforeDispatching)
      println("context became")
      //resetEPStatements
      self ! UnregisterAllEvents
    // TODO: evt@_ somewhat generic and hence Received Buy is logged 12 times when it's above case UnregisterAllEvents
    case evt@_ =>
      println(s"CASE EVT DISPATCH: ${evt}")
      esperEngine.epRuntime.sendEvent(evt)
    //case _ =>
  }

  private def shutdown = {
    context.stop(self)
  }
}


