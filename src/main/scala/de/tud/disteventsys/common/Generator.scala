package de.tud.disteventsys.common

import de.tud.disteventsys.esper.EsperStream

import scala.util.Random

/**
  * Created by ms on 02.01.17.
  */
class Generator[+T] {
  //self =>

  //def getClassName: T
}
//TODO: would be nice to have map or flatmap

/*case object BuyGenerator extends Generator[String]{
  def getClassName = "Buy"
}*/
case class BuyGenerator(clz: String = "Buy") extends Generator[String]
case class PriceGenerator(clz: String = "Price") extends Generator[String]{
  self =>
  object symbol {
    implicit class PriceThings(x: PriceGenerator) {
      object symbol {
        def :=() = {
          // refer generator type here and create string
          // example Price.symbol = BP
        }
      }
    }
    def :=(s: String) = {
      self.equals = s
      hasEquals = true
      equalTo = s"symbol = $s"
    }
  }
  object amount {
    def :=(a: Int) = {
      self.equals = a
      hasEquals = true
      equalTo = s"amount = $a"
    }
    def >(a: String) = {
      greaterThan = s"amount > $a"
    }
  }
  var uniqueField = ""
  var equalTo = ""
  var greaterThan = ""
  var hasEquals: Boolean = false
  var equals: Any = _
  var windowLength: Int = _
  def hasUniqueField: Boolean = !uniqueField.isEmpty
  def getFields = Array("symbol", "price", "amount")
  def setUniqueField(uf: String) = uniqueField = uf
  def getUniqueField = uniqueField
  def equals(x: Int): Unit = {
    equals = x
    hasEquals = true
  }
  def getEquals = equals
  def withUniqueField(uf: String) = {
    //uniqueField = uf
  }
  def withUnique(f: Array[String] => String) = {
    //uniqueField = f(getFields)
  }
}
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
