package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.concurrent.{Await, Future}
import de.tud.disteventsys.actor.EsperActor._
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

trait ActorCreator {
  private lazy val system = ActorSystem()
  private lazy val esperActor = system.actorOf(Props(classOf[EsperActor]), "esper")
  private lazy val buyerActor = system.actorOf(Props(classOf[BuyerActor]), "buyer")
  private lazy val priceActor = system.actorOf(Props(classOf[PriceActor]), "pricer")
  private lazy val sellerActor = system.actorOf(Props(classOf[SellerActor]), "seller")
  private val actors: Map[String, ActorRef] = Map(
    buyerActor.path.name  -> buyerActor,
    priceActor.path.name  -> priceActor,
    sellerActor.path.name -> sellerActor
  )
  private var variableStream: Stream[_] = _

  def process[T](eplStringBuilder: StringBuilder, node: Tree[T]): Future[Stream[T]] = Future {
    // initialize statement
    val statement = new Statement()
    statement.initEpl(eplStringBuilder)
    val orderSize = 1000
    println(s"STATEMTNE: $eplStringBuilder")
    val eplStatement = statement.getEplStatement

    esperActor ! InitializeActors(actors)

    statement.getAllEvents foreach {
      case (clz, underlyingClass) =>
        esperActor ! RegisterEventType(clz, underlyingClass)
    }
    esperActor ! DeployStatements(Array(eplStatement), statement.getEventsList, None, statement.getResponsibleEvent)
    esperActor ! StartProcessing

    // run test data
    dummyData

    Stream(statement, node)
  }

  def processWithStreams[T](sb: StringBuilder, streams: Array[Stream[_]], node: Tree[T]) = {
    val statement = new Statement()
    statement.initEpl(sb)
    val stream = streams.head
    val currentEplString = statement.getEplStatement
    val newStream = new Stream[T](statement, node)

    val oldStatements = streams map { s => s.getStatement }
    val oldEplStrings = oldStatements map { os => os.getEplStatement }
    // unregister current esperActor Events and merge current eplString with that of String
    // by creating anonymous actors to handle
    //println(s"ABOUT TO UNREGISTER EVENTS: ${esperActor.path.getElements}")
    // unregister all events
    esperActor ! UnregisterAllEvents

    esperActor ! InitializeActors(actors)
    // add old statements with new ones
    var maps: Map[String, Class[_]] = Map.empty
    oldStatements foreach { os => maps = maps ++ os.getAllEvents}
    maps = maps ++ statement.getAllEvents
    maps foreach {
      case (clz, underlyingClass) =>
        println(s"REGISTER EVEnt TYPE 2: $clz, $underlyingClass")
        esperActor ! RegisterEventType(clz, underlyingClass)
    }
    // get events list for all old statements
    // excluding current eplString because it depends on the above
    val eventsList = (oldStatements map { os => os.getEventsList }).flatten.toList
    val eventsWithFields = newStream.getEventWithFields
    esperActor ! DeployStatements(oldEplStrings, eventsList, Some(eventsWithFields), oldStatements(0).getResponsibleEvent)
    esperActor ! StartProcessing

    println(s"before returning variable Stream: $newStream")
    dummyData
    newStream
  }

  private def dummyData = {
    val prices = Array(
      Price("BP", 7.61), Price("RDSA", 2101.00), Price("RDSA", 2209.00),
      Price("BP",7.66), Price("BP", 7.64), Price("BP", 7.67)
    )

    prices foreach (esperActor ! _)
  }
}
