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

  case class InitializeActors(actors: Map[String, ActorRef])

  //case class DeployStatement(eplStatement: String, name: String)

  case class CreateActor(clz: String)

  case class ReceiveCreatedActor(actor: ActorRef)

  case object UnregisterAllEvents

  case class DeployStream(eventWithFields: Tuple2[String, List[String]])

  case class DeployStatements(eplStatements: Array[String], eventsList: List[String],
                                eventWithFields: Option[Tuple2[String, List[String]]], actionEvent: String)

}


class EsperActor extends Actor with ActorLogging with EsperEngine{
  import EsperActor._
  import scala.util.Random
  val rand = new Random()
  // timeout period before which an event depending on the currently fired event should fire
  // time constraints
  val eventTimeoutDuration: FiniteDuration = 100 millis

  private var createdActors: List[ActorRef] = List.empty
  private var currentEsperStatement: EPStatement = _
  private var handlers: Array[ActorRef] = Array.empty
  private var actors: Map[String, ActorRef] = Map.empty
  private var EPStatements: Array[Try[EPStatement]] = Array.empty
  private var EPStatement: Try[EPStatement] = _
  private val delay = 10 seconds
  private var currentMainHandler: ActorRef = _

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
      val eventTypes = esperConfig.getEventTypeNames.keySet()
      if(!eventTypes.contains(name)){
        //println(s"EVENT NOT YET REGISTERED, now registering: $name")
        esperConfig.addEventType(name, clz.getName)
      }

    case InitializeActors(mActors: Map[String, ActorRef]) =>
      if(actors.keys.size == 0){
        actors = actors ++ mActors
        //println(s"ACTIRS: $actors")
      }

    case StartProcessing                =>
      context.become(dispatchingToEsper)

    case DeployStatements(eplStatements, eventsList, eventWithFields, actionEvent) =>
      eventWithFields match {
        case Some(evtwf) =>
          killChildActor
          val helperHandler = context.actorOf(HelperHandler.props(self, actors, evtwf))
          currentMainHandler = context.actorOf(MainHandler.props(self, actors, eventsList,
            Some(helperHandler), actionEvent, eventTimeoutDuration), s"handler${rand.nextLong()}")
          executeStatements(eplStatements, currentMainHandler)
        case None =>
          currentMainHandler = context.actorOf(MainHandler.props(self, actors, eventsList,
            None, actionEvent, eventTimeoutDuration), s"handler${rand.nextLong()}")
          executeStatements(eplStatements, currentMainHandler)
      }

    case UnregisterAllEvents =>
      resetEPStatements
  }
  // create EPL statement for each query defined
  private def executeStatements(eplStatements: Array[String], mainHandler: ActorRef) = {
    eplStatements foreach {
      es =>
        EPStatement = createEPL(es){evt => mainHandler ! evt }
        //println(s"EPStatement created: $EPStatement")
        EPStatements = EPStatements :+ EPStatement
    }

  }

  private def killChildActor = {
    context.stop(currentMainHandler)
  }

  private def resetEPStatements = {
    EPStatements foreach {
      tryStat =>
        val stat = tryStat.get
        println(s"STAT FOUND, UNREGISTERING: ${stat} $EPStatement")
        stat.destroy()
    }
    EPStatements = Array.empty
    handlers = Array.empty
  }

  private def dispatchingToEsper(): Receive = {
    case UnregisterAllEvents =>
      context.become(beforeDispatching)
      self ! UnregisterAllEvents
    case evt@_ =>
      println(s"CASE EVT DISPATCH: ${evt}")
      epRuntime.sendEvent(evt)
  }

  private def shutdown = {
    context.stop(self)
  }
}


