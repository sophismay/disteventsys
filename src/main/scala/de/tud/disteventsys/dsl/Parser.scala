package de.tud.disteventsys.dsl

/**
  * Created by ms on 03.12.16.
  */
import de.tud.disteventsys.dsl.QueryAST.Operator
import fastparse.all._

import scala.util.parsing.combinator._

trait Parser[+T] {
  import Grammar._

  def parse[A >: T](tree: Tree[A]) = Grammar.parseAll(tree)
}

trait ClauseHelper {
  def join(default: String, fields: List[String]): String = {
    fields match {
      case Nil          => default
      case head :: tail => s"${head}, ${join(default, tail)}"
    }
  }
}

trait Clause extends JavaTokenParsers with ClauseHelper{
  /* parent clause is the first command to execute to esper engine when the
     corresponding query is fired or matched.
     eg. insert into Buy <---- parent clause
         select * from ...
   */
  import QueryAST._

  def parentClause

  def selectClause(fields: List[String]): Parser[Operator => Operator] = {
    val fieldsString = join("*", fields)
    "select" ~> fieldsString
  }

  def whereClause(expr: Expr): Parser[Operator => Operator] = {
    expr match {
      
    }
  }

  def fromClause
}

object Grammar extends Clause{

  def statement =

  def parseAll[A](tree: Tree[A]) = {
    tree.toList foreach {
      t =>

    }
  }
}