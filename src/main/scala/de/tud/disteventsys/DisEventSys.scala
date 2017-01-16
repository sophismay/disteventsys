package de.tud.disteventsys

import akka.actor.{ActorSystem, Props}
import de.tud.disteventsys.actor.{BuyerActor, EsperActor}
import de.tud.disteventsys.actor_classes.{Buy, Price}
import de.tud.disteventsys.config.Config
import de.tud.disteventsys.actor.EsperActor._
import de.tud.disteventsys.dsl.QueryDSL
import de.tud.disteventsys.actor_classes.{ BuyGenerator, PriceGenerator, FieldsGenerator }

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
    val buyClass = BuyGenerator.getClassName
    val priceClass = PriceGenerator.getClassName
    val fields = FieldsGenerator("a, b").getFields

    val currentDsl = dsl INSERT buyClass SELECT fields FROM priceClass
    val stream1 = currentDsl.createStream
    println(s"STREAM 1: ${stream1}")
    
  }
}
