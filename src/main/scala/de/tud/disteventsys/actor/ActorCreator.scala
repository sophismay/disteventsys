package de.tud.disteventsys.actor

import akka.actor.{ActorSystem, Props}
import de.tud.disteventsys.actor.EsperActor.{DeployStatement, RegisterEventType, StartProcessing}
import de.tud.disteventsys.actor.BuyerActor
import de.tud.disteventsys.actor_classes.{Buy, Price}

/**
  * Created by ms on 14.12.16.
  */
// infer classes from epl String
// create esperActor
// create other actors
// register, deploy statement and start processing

trait ActorCreator {
  //private var eplString: String = _
  private lazy val system = ActorSystem()
  private lazy val esperActor = system.actorOf(Props(classOf[EsperActor]))
  private lazy val buyer = system.actorOf(Props(classOf[BuyerActor]))

  def process(eplStatement: String) = {
    val orderSize = 1000
    val statement =
      s"""
        insert into Buy
        select p.symbol, p.price, $orderSize
        from Price.std:unique(symbol) p
        """

    //TODO: infer classes from statement
    esperActor ! RegisterEventType("Price", classOf[Price])
    esperActor ! RegisterEventType("Buy", classOf[Buy])
    esperActor ! DeployStatement(statement, Some(buyer))
    esperActor ! StartProcessing

    dummyData
  }

  def dummyData = {
    val prices = Array(
      Price("BP", 7.61), Price("RDSA", 2101.00), Price("RDSA", 2209.00),
      Price("BP",7.66), Price("BP", 7.64), Price("BP", 7.67)
    )

    prices foreach (esperActor ! _)
  }
}
