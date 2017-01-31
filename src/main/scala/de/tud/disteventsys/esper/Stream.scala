package de.tud.disteventsys.esper

import de.tud.disteventsys.dsl.QueryDSL

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
  * Created by ms on 31.01.17.
  */

class Stream(val statement: Statement){

  /*def events(f: Iterable[String] => Boolean, timeout: FiniteDuration = 1 second):  = {

    if(f(getAllEvents.keys))
  }*/
}

object Stream{
  def apply(statement: Statement): Stream = new Stream(statement)
}
