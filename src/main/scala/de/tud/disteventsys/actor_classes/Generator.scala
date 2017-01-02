package de.tud.disteventsys.actor_classes

/**
  * Created by ms on 02.01.17.
  */
trait Generator[T] {
  self =>

  //def getClassName: T
}

case object BuyGenerator extends Generator[String]{
  def getClassName = "Buy"
}

case object PriceGenerator extends Generator[String]{
  def getClassName = "Price"
}

case object SellGenerator extends Generator[String]{
  def getClassName = "Sell"
}

case class FieldsGenerator(val fields: String) extends Generator[Seq[String]]{
  //def getClassName = ""
  def getFields = {
    fields.split(",").map{ f => f.trim }.toList
  }
}
