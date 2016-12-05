package de.tud.disteventsys

import akka.actor.{ActorSystem, Props}
import de.tud.disteventsys.actor.{BuyerActor, EsperActor}
import de.tud.disteventsys.actor_classes.{Buy, Price}
import de.tud.disteventsys.config.Config
import de.tud.disteventsys.actor.EsperActor._
import de.tud.disteventsys.dsl.QueryDSL

import scala.collection.immutable.RedBlackTree
/**
  * Created by ms on 23.11.16.
  */
object DisEventSys extends App {

  val optionsParser = new scopt.OptionParser[Config]("diseventsys") {
    head("diseventsys", "1.0")

    opt[String]('o', "option1").action((x, c) =>
      c.copy(option1 = x)).text("Option 1 is ....")

    help("help").text("prints this usage text")

    //cmd("update").action( (_, c) => c.copy(mode = "update") ).
    //  text("update is a command.").children(???)

  }

  override def main(args: Array[String]) = {
    /*optionsParser.parse(args, Config()) match {
      case Some(config) => println("SOME")

      case None => println("NONE")
    }*/

    val dsl = QueryDSL()
    dsl SELECT "*" SELECT "*" INSERT "buy"

    val orderSize = 1000

    val system = ActorSystem()
    val esperActor = system.actorOf(Props(classOf[EsperActor]))
    val buyer = system.actorOf(Props(classOf[BuyerActor]))

    val statement =
      s"""
        insert into Buy
        select p.symbol, p.price, $orderSize
        from Price.std:unique(symbol) p
        """

    esperActor ! RegisterEventType("Price", classOf[Price])
    esperActor ! RegisterEventType("Buy", classOf[Buy])
    esperActor ! DeployStatement(statement, Some(buyer))
    esperActor ! StartProcessing

    val prices = Array(
      Price("BP", 7.61), Price("RDSA", 2101.00), Price("RDSA", 2209.00),
      Price("BP",7.66), Price("BP", 7.64), Price("BP", 7.67)
    )

    prices foreach (esperActor ! _)
  }
}
