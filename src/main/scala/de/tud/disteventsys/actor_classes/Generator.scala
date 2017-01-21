package de.tud.disteventsys.actor_classes

import de.tud.disteventsys.esper.EsperStream

import scala.util.Random

/**
  * Created by ms on 02.01.17.
  */
class Generator[T] {
  //self =>

  //def getClassName: T
}
//TODO: would be nice to have map or flatmap

/*case object BuyGenerator extends Generator[String]{
  def getClassName = "Buy"
}*/
case class BuyGenerator(clz: String = "Buy") extends Generator[String]
case class PriceGenerator(clz: String = "Price") extends Generator[String]
case class SellGenerator(clz: String="Sell") extends Generator[String]

case object StreamReferenceGenerator extends Generator[String]{
  var ref: String = ""
  def generateReference = {
    val rand = new Random()
    ref += rand.nextLong()
    println(s"STREAM REFERENCE: $ref")
    ref
  }
}

/*case object PriceGenerator extends Generator[String]{
  def getClassName = "Price"
}*/

/*case object SellGenerator extends Generator[String]{
  def getClassName = "Sell"
}*/

case class FieldsGenerator(val fields: String) extends Generator[Seq[String]]{
  //def getClassName = ""
  def getFields = {
    fields.split(",").map{ f => f.trim }.toList
  }
}

case class EsperStreamGenerator(es: EsperStream.type) extends Generator[EsperStream.type]{

}

/*object Generator {
  def unapply[T](g: Generator[T]): Option[Generator[T]] = Some(g)
}*/
