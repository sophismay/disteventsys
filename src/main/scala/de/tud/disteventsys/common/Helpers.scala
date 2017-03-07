package de.tud.disteventsys.common

/**
  * Created by ms on 07.03.17.
  */
object Helpers {
  implicit class GeneratorWithWhere[T](gen: Generator[T]) {
    def where(f: Generator[T] => String) = {
      println(s"INSIDE WHERE")
      val r = f(gen)
      println(s"RSLT: $r")
    }
  }
}
