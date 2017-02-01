package de.tud.disteventsys.esper

import de.tud.disteventsys.dsl.{QueryDSL, Tree}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
  * Created by ms on 31.01.17.
  */

class Stream[T](val statement: Statement, node: Tree[T]){
  def getStatement = statement
  def getTree = node
  /*def events(f: Iterable[String] => Boolean, timeout: FiniteDuration = 1 second):  = {

    if(f(getAllEvents.keys))
  }*/
}

object Stream{
  def apply[T](statement: Statement, node: Tree[T]) = new Stream(statement, node)
}
