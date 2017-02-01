package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.concurrent.Future
import de.tud.disteventsys.actor.EsperActor._
import de.tud.disteventsys.actor.BuyerActor
import de.tud.disteventsys.dsl.Tree
import de.tud.disteventsys.esper.Statement
import de.tud.disteventsys.event.Event._
import de.tud.disteventsys.esper.Stream

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ms on 14.12.16.
  */
// infer classes from epl String
// create esperActor
// create other actors
// register, deploy statement and start processing

trait ActorSystemInitiator{
  lazy val system = ActorSystem()
  lazy val esperActor = system.actorOf(Props(classOf[EsperActor]))
}

class Creator extends Actor with ActorSystemInitiator{
  //private lazy val actor =
  private val actor = system.actorOf(Props(classOf[Creator]))

  def receive: Receive = {

    case EsperEvent(className, underlying) =>
      println(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case Buy(s, p, a) =>
          println(s"Received Buy: ${s}, ${p}, ${a}")
      }
    case _ => println(s"Could not find a corresponding case class")
  }

  def getActor = {
    actor
  }
}

trait ActorCreator {
  //private var eplString: String = _
  private lazy val system = ActorSystem()
  private lazy val esperActor = system.actorOf(Props(classOf[EsperActor]))

  def process[T](eplStringBuilder: StringBuilder, node: Tree[T]): Future[Stream[T]] = Future {
    // initialize statement
    val statement = new Statement()
    statement.initEpl(eplStringBuilder)
    val orderSize = 1000
    println(s"STATEMTNE: $eplStringBuilder")
    val eplStatement = statement.getEplStatement
    /*val statement =
      s"""
        insert into Buy
        select p.symbol, p.price, $orderSize
        from Price.std:unique(symbol) p
        """*/

    //TODO: infer classes from statement
    // DONE
    statement.getAllEvents foreach {
      case (clz, underlyingClass) =>
        esperActor ! RegisterEventType(clz, underlyingClass)
        esperActor ! CreateActor(clz)
    }
    esperActor ! DeployStatements(eplStatement)
    // TODO: make statement a trait so that one can not only infer the eplString but also the classes, etc
    //TODO: infer actor(eg. buyer) from statement
    //esperActor ! RegisterEventType("Price", classOf[Price])
    //esperActor ! RegisterEventType("Buy", classOf[Buy])
    //esperActor ! CreateActor("Buy")
    //esperActor ! DeployStatement(eplStatement, "buyer")
    // could deploy statements on multiple actors, then return actors, not esperActor
    //esperActor ! DeployStatement(eplStatement, Some(buyer))
    esperActor ! StartProcessing

    dummyData

    //TODO: create separate actor each time its called
    Some(esperActor)
    //Some(buyer)
    //TODO: return instead stream
    Stream(statement, node)
  }

  def processWithStreams(sb: StringBuilder, streams: Array[Stream[_]]) = {
    val stream = streams.head
    // unregister current esperActor Events and merge current eplString with that of String
    // next, include timeout
    esperActor ! UnregisterAllEvents
  }

  private def dummyData = {
    val prices = Array(
      Price("BP", 7.61), Price("RDSA", 2101.00), Price("RDSA", 2209.00),
      Price("BP",7.66), Price("BP", 7.64), Price("BP", 7.67)
    )

    prices foreach (esperActor ! _)
  }
}
