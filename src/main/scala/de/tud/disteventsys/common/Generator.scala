package de.tud.disteventsys.common

import de.tud.disteventsys.esper.EsperStream

import scala.util.Random

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
      /*self.equals = a
      hasEquals = true
      equalTo = s"price = $a"
      clause = s"price = $a"*/
    }
    def >(a: Int) = {
      self.>(a.toString, field)
    }
  }
}

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
  /*def equals(x: Int): Unit = {
    equals = x
    hasEquals = true
  }*/
  //def getEquals = equals
}
case class SellGenerator(clz: String="Sell") extends Generator[String] with Helpers {
  def getFields = Array("symbol", "price", "amount")
}

case object StreamReferenceGenerator extends Generator[String]{
  var ref: String = ""
  def generateReference = {
    val rand = new Random()
    ref += rand.nextLong()
    println(s"STREAM REFERENCE: $ref")
    ref
  }
}

case class FieldsGenerator(val fields: String) extends Generator[Seq[String]]{
  //def getClassName = ""
  def getFields = {
    fields.split(",").map{ f => f.trim }.toList
  }
}

case class EsperStreamGenerator(es: EsperStream.type) extends Generator[EsperStream.type]{

}
