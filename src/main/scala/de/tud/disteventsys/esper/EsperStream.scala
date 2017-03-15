package de.tud.disteventsys.esper

/**
  * Created by ms on 02.01.17.
  */
// a general representation of the actor, and it's epl statement

case class EsperStream[T](stream: Stream[T]){
  def statement = stream.getStatement
}