package de.tud.disteventsys.dsl

/**
  * Created by ms on 03.12.16.
  */
import de.tud.disteventsys.dsl.QueryAST._
import de.tud.disteventsys.esper.EsperStream
import fastparse.all._
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.util.parsing.combinator._

trait ClauseHelper {
  def join(default: String, fields: List[String]): String = {
    fields match {
      case Nil          => default
      case head :: tail => if(tail.isEmpty) s"$head" else  s"$head, ${join(default, tail)}"
    }
  }
}

trait Clause extends JavaTokenParsers with ClauseHelper {
  /* parent clause is the first command to execute to esper engine when the
     corresponding query is fired or matched.
     eg. insert into Buy <---- parent clause
         select * from ...
   */

  def parentClause(op: Operator): String = {
    op match {
      case Insert(stream) => insertClause(stream)
      case _              => throw new NotImplementedError()
    }
  }

  def selectClause(fields: List[String]): String = {
    val fieldsString = join("*", fields)
    s"select ${fieldsString}\n"
  }

  def insertClause(clz: String): String = {
    s"insert into ${clz.capitalize}\n"
  }

  def whereClause(expr: Expr): String = {
    val clause = evaluateExpr(expr)
    s"where ${clause}\n"
  }

  def fromClause(clz: String): String = {
    s"from ${clz.capitalize}\n"
  }

  def fromStreamClause[A](stream: EsperStream[A]) = {
    // TODO; reference to stream?
    s"from ${stream.getStreamReference}"
  }
}

trait Parser[+T] {

  def parse[A >: T](tree: Tree[A]) = Grammar.parseAll(tree)

  object Grammar extends Clause{

    private var eplString = new StringBuilder()

    private def statement[A](tree: Tree[A]) = {
      tree match {
        case NonEmptyTree(d, _, _) =>
          d match {
            case Insert(stream)     => insertClause(stream)
            case Select(fields)     => selectClause(fields)
            case From(clz)          => fromClause(clz)
            case FromStream(stream) => fromStreamClause(stream)
            case _                  => "" //throw new NotImplementedException()
          }
        case _                     => throw new NotImplementedException()
      }
    }

    def parseAll[A](tree: Tree[A]) = {
      tree.toList foreach {
        t =>
          eplString = new StringBuilder(eplString + statement(t))
      }
      eplString
    }
  }
}