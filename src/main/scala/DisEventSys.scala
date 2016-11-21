/**
  * Created by ms on 16.11.16.
  */

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import EsperActor._
import akka.actor.Actor.Receive

import scala.beans.BeanProperty

/*
Parsing command line options
 */
case class Config(option1: String)

case class Price(@BeanProperty symbol: String, @BeanProperty price: Double)
case class Buy(@BeanProperty symbol: String, @BeanProperty price: Double, @BeanProperty amount: Long)

class BuyerActor extends Actor with ActorLogging {
  override def receive: Receive = {

    case EsperEvent(className, underlying) =>
      println(s"CASE ESPEREVENT ${className}:")
      underlying match {
        case Buy(s, p, a) =>
          println(s"Received Buy: ${s}, ${p}, ${a}")
      }
    case _ => println(s"Could not find a corresponding case class")
  }
}

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
