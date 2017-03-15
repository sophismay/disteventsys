package de.tud.disteventsys.dsl

/**
  * Created by ms on 03.12.16.
  */
import de.tud.disteventsys.dsl.QueryAST._
import sun.reflect.generics.reflectiveObjects.NotImplementedException

trait ClauseHelper {
  def join(default: String, fields: List[String]): String = {
    fields match {
      case Nil          => default
      case head :: tail => if(tail.isEmpty) s"$head" else  s"$head, ${join(default, tail)}"
    }
  }
}

trait Clause extends ClauseHelper {
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

  /*def whereClause(expr: Expr): String = {
    val clause = evaluateExpr(expr)
    s"where ${clause}\n"
  }*/

  def fromClause(clz: String, extras: Option[Map[String, String]]): String = {
    val ext = extras.getOrElse(Map.empty)
    var augmented = ""
    if(!ext.isEmpty) {
      // get keys, that is, esper-specific
      val keys = ext map { _. _1 }
      keys foreach {
        k =>
          if(k == "unique") {
            augmented = addUniqueClause(ext(k))
          }
      }
    }

    println(s"FROM CLAUSE: ${augmented} $ext ${s"from ${clz.capitalize}$augmented\n"}")
    if(augmented.isEmpty) s"from ${clz.capitalize}\n" else s"from ${clz.capitalize}$augmented\n"
    //s"from ${clz.capitalize}\n"
  }

  def whereClause(clz: String, clause: String): String = {
    s"where $clz.$clause\n"
  }

  private def addUniqueClause(field: String): String = {
    s".std:unique($field)"
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
            case From(clz, extras, _)          => fromClause(clz, extras)
            case Where(clz, field) => whereClause(clz, field)
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
      val tempEplString = eplString
      eplString = new StringBuilder()
      tempEplString
    }
  }
}