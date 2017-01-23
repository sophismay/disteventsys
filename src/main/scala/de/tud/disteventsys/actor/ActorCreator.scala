package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import de.tud.disteventsys.actor.EsperActor.{DeployStatement, RegisterEventType, StartProcessing}
import de.tud.disteventsys.actor.BuyerActor
import de.tud.disteventsys.actor_classes.{Buy, Price}
import de.tud.disteventsys.event.EsperEvent

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
  private lazy val buyer = system.actorOf(Props(classOf[BuyerActor]))
  private lazy val seller = system.actorOf(Props(classOf[SellerActor]))
  private lazy val price = system.actorOf(Props(classOf[PriceActor]))
  //private lazy val creator =
  //val buyer = creator.getActor
  def process(eplStatement: String) = {
    val orderSize = 1000
    println(s"STATEMTNE: $eplStatement")
    val statement =
      s"""
        insert into Buy
        select p.symbol, p.price, $orderSize
        from Price.std:unique(symbol) p
        """

    //TODO: infer classes from statement
    // TODO: make statement a trait so that one can not only infer the eplString but also the classes, etc
    //TODO: infer actor(eg. buyer) from statement
    esperActor ! RegisterEventType("Price", classOf[Price])
    esperActor ! RegisterEventType("Buy", classOf[Buy])
    // could deploy statements on multiple actors, then return actors, not esperActor
    esperActor ! DeployStatement(eplStatement, Some(buyer))
    esperActor ! StartProcessing

    dummyData

    //TODO: create separate actor each time its called
    Some(buyer)
  }

  def dummyData = {
    val prices = Array(
      Price("BP", 7.61), Price("RDSA", 2101.00), Price("RDSA", 2209.00),
      Price("BP",7.66), Price("BP", 7.64), Price("BP", 7.67)
    )

    prices foreach (esperActor ! _)
  }
}
