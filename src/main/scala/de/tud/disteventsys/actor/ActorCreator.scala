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
  private lazy val esperActor = system.actorOf(Props(classOf[EsperActor]), "esper")
  private lazy val buyerActor = system.actorOf(Props(classOf[BuyerActor]), "buyer")
  private lazy val priceActor = system.actorOf(Props(classOf[PriceActor]), "pricer")
  private lazy val sellerActor = system.actorOf(Props(classOf[SellerActor]), "seller")
  private val actors: Map[String, ActorRef] = Map(
    buyerActor.path.name  -> buyerActor,
    priceActor.path.name  -> priceActor,
    sellerActor.path.name -> sellerActor
  )

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
    println(s"NAMEOFACTOR ${buyerActor.path.name}")
    esperActor ! InitializeActors(actors)

    // DONE
    statement.getAllEvents foreach {
      case (clz, underlyingClass) =>
        esperActor ! RegisterEventType(clz, underlyingClass)
        //esperActor ! CreateActor(clz)
    }
    //esperActor ! DeployStatements(eplStatement)
    esperActor ! DeployStatementsss(Array(eplStatement), statement.getEventsList, None)
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
    //Some(esperActor)
    //Some(buyer)
    //TODO: return instead stream
    Stream(statement, node)
  }

  def processWithStreams[T](sb: StringBuilder, streams: Array[Stream[_]], node: Tree[T]) = {
    val statement = new Statement()
    statement.initEpl(sb)
    val stream = streams.head
    val currentEplString = statement.getEplStatement
    val newStream = new Stream[T](statement, node)
    //val oldStatement = stream.getStatement
    //val oldEplString = oldStatement.getEplStatement

    val oldStatements = streams map { s => s.getStatement }
    val oldEplStrings = oldStatements map { os => os.getEplStatement }
    //val (oldStatements, oldEplStrings) = streams map { s => (s.getStatement, s.getStatement.getEplStatement)}
    // unregister current esperActor Events and merge current eplString with that of String
    // by creating anonymous actors to handle
    // next, include timeout
    println(s"ABOUT TO UNREGISTER EVENTS: ${esperActor.path.getElements}")
    // stop events or unregister?
    esperActor ! UnregisterAllEvents
    println(s"AFTER ABOUT TO UNREGISTER EVENTS")

    esperActor ! InitializeActors(actors)
    // add old statements with new ones
    var maps: Map[String, Class[_]] = Map.empty
    oldStatements foreach { os => maps = maps ++ os.getAllEvents}
    maps = maps ++ statement.getAllEvents
    //val a = oldStatements map { os: Statement => os.getAllEvents }
    //val s = a ++ statement.getAllEvents
    println(s"MAP OLD + NEW: ${maps}")
    maps foreach {
      case (clz, underlyingClass) =>
        println(s"REGISTER EVEnt TYPE 2: $clz, $underlyingClass")
        esperActor ! RegisterEventType(clz, underlyingClass)
    }

    // get events list for all old statements
    // excluding current eplString because it depends on the above
    println("befor massacre")
    val eventsList = (oldStatements map { os => os.getEventsList }).flatten.toList
    //eventsList foreach { e => println(s"EVENTSLIST FL: $e")}
    println(s"EVENTSLIST FLATTEN: ${eventsList} ${newStream.getEventWithFields}")
    val eventsWithFields = newStream.getEventWithFields
    println("BEFORE CALLING DEPLOY STATEMENTSSS 2nd call")
    esperActor ! DeployStatementsss(oldEplStrings, eventsList, Some(eventsWithFields))
    //esperActor ! DeployStream(newStream.getEventWithFields)
    esperActor ! StartProcessing
    dummyData
    //Stream(statement, node)
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
