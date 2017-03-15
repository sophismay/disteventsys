package de.tud.disteventsys.common

import de.tud.disteventsys.esper.EsperStream

import scala.util.Random

// where clauses helper
trait Helpers {
  self =>

  var uniqueField = ""
  var equalTo = ""
  var greaterThan = ""
  var hasEquals: Boolean = false
  var equals: Any = _
  var clause = ""

  private def :=(a: String, field: String) = {
    self.equals = a
    hasEquals = true
    //equalTo = s"$field = $a"
    clause = s"$field = $a"
  }
  private def >(a: String, field: String) = {
    clause = s"$field > $a"
  }

  object symbol {
    private val field = "symbol"
    def :=(a: String) = {
      self.:=(a, field)
    }
  }
  object amount {
    private val field = "amount"
    def :=(a: Int) = {
      self:=(a.toString, field)
    }
    def >(a: String) = {
      self.>(a, field)
    }
  }

  object price {
    private val field = "price"
    def :=(a: Int) = {
      self.:=(a.toString, field)
    }
    def >(a: Int) = {
      self.>(a.toString, field)
    }
  }
}

// class to Generate Events in a type-safe way
// create also "where" clauses and also obtain existing fields for each Event
class Generator[T] {}

case class BuyGenerator(clz: String = "Buy") extends Generator[String] with Helpers {
  def getFields = Array("symbol", "price", "amount")
}
case class PriceGenerator(clz: String = "Price") extends Generator[String] with Helpers{
  var windowLength: Int = _
  def hasUniqueField: Boolean = !uniqueField.isEmpty
  def getFields = Array("symbol", "price", "amount")
  def setUniqueField(uf: String) = uniqueField = uf
  def getUniqueField = uniqueField
}
case class SellGenerator(clz: String="Sell") extends Generator[String] with Helpers {
  def getFields = Array("symbol", "price", "amount")
}

case class FieldsGenerator(val fields: String) extends Generator[Seq[String]]{
  def getFields = {
    fields.split(",").map{ f => f.trim }.toList
  }
}
case class EsperStreamGenerator(es: EsperStream.type) extends Generator[EsperStream.type]{}