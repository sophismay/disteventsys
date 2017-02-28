package de.tud.disteventsys.esper

import de.tud.disteventsys.dsl.QueryAST.Select
import de.tud.disteventsys.dsl.{NonEmptyTree, QueryDSL, Tree, EmptyTree}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
  * Created by ms on 31.01.17.
  */

class Stream[T](val statement: Statement, node: Tree[T]){
  def getStatement = statement
  def getTree = node
  def getEventWithFields: Tuple2[String, List[String]] = {
    println(s"GET EVENT WITH FIELDS CALLED: ${node}")
    val rslt = getFirstEventWithFields(node)
    println(s"GEWF: ${rslt}")
    rslt
  }
  // get stream event to write to as well as fields to be written
  private def getFirstEventWithFields(n: Tree[T]): Tuple2[String, List[String]] = {
    n match {
      case NonEmptyTree(d, l, r) =>
        d match {
          case Select(fields) =>
            (statement.getResponsibleEvent, fields)
          case _ =>
            getFirstEventWithFields(r)
        }
      case EmptyTree => ("", List.empty)
    }
  }
  /*def events(f: Iterable[String] => Boolean, timeout: FiniteDuration = 1 second):  = {

    if(f(getAllEvents.keys))
  }*/
}

object Stream{
  def apply[T](statement: Statement, node: Tree[T]) = new Stream(statement, node)
}
