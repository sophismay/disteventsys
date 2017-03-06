package de.tud.disteventsys

import akka.actor.{ActorSystem, Props}
import de.tud.disteventsys.actor.{BuyerActor, EsperActor}
import de.tud.disteventsys.event.Event._
import de.tud.disteventsys.config.Config
import de.tud.disteventsys.actor.EsperActor._
import de.tud.disteventsys.common.{BuyGenerator, FieldsGenerator, PriceGenerator, SellGenerator}
import de.tud.disteventsys.dsl.QueryDSL

import scala.collection.immutable.RedBlackTree
/**
  * Created by ms on 23.11.16.
  */
object DisEventSys extends App {

  lazy val optionsParser = new scopt.OptionParser[Config]("diseventsys") {
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
    // TODO: implicit reference to dsl
    //val buyClass = BuyGenerator.getClassName
    //val priceClass = PriceGenerator.getClassName
    //val fields = FieldsGenerator("a, b").getFields

    val buy = BuyGenerator()
    val price = PriceGenerator()
    val sell = SellGenerator()
    val fields = FieldsGenerator("symbol, price, 100")

    val currentDsl = dsl INSERT buy SELECT fields FROM price
    val queryResult = currentDsl.createQuery
    println(s"RSLT from createQuery: $queryResult")
    val nextDsl = dsl INSERT sell SELECT fields FROM price
    nextDsl.createQuery
    //val stream1 = currentDsl.createStream
    //println(s"STREAM RETURNED: ${stream1.statement}")
    // now create stream from existing stream
    // stream1/dsl INSERT buy SELEcT fields FrOM stream1.events(f: Tuple => Boolean, timeout)
    //val nextDsl =  dsl INSERT sell SELECT fields FROM stream1
    //val stream2 = nextDsl.createStream
    //println(s"STREAM 2: ${stream2}")

    // create stream from joining two streams
    
  }
}
