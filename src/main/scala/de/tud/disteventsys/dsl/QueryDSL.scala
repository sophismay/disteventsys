package de.tud.disteventsys.dsl

import scala.util.{Failure, Success, Try}


/**
  * Created by ms on 21.11.16.
  */

object QueryAST {
  //type Stream
  //type Schema = Vector[String]

  // Parent Operator for Esper Stream
  sealed abstract class ParentOperator
  // clz: class to insert into
  case class INSERT(clz: String) extends ParentOperator

  // Operators such as select
  sealed abstract class Operator
  //case class Select(parent: ParentOperator, fields: List[String]) extends Operator
  case class Select(fields: List[String])                   extends Operator
  case class Insert(stream: String)                         extends Operator
  case class From(clz: String)                              extends Operator
  //case class From(parent: Operator, clz: String)            extends Operator
  case class Where(expr: Expr)                              extends Operator

  // Expressions/ Filters
  abstract sealed class Expr
  case object NoExpr                     extends Expr
  //case class Literal(value: Any)         extends Expr
  case class Literal(value: String)         extends Expr
  case class Equal(left: Expr, right: Expr) extends Expr

  // References
  sealed abstract class Ref
  case class Field(name: String) extends Ref
  case class Value(value: Any)   extends Ref

  // smart constructors
  def evaluateExpr(expr: Expr): String = {
    expr match {
      case NoExpr          => ""
      case Literal(value)  => value
      case Equal(l, r)     => s"${evaluateExpr(l)} = ${evaluateExpr(r)}"
    }
  }
}


class QueryDSL extends Parser[Tree[Any]]{
  self =>

  import Tree._
  import QueryAST._

  private var eplString: String = _
  private val rootNode = empty
  //private val rootNode = node("root")
  private var currentNode: Tree[Any] = rootNode

  private def treeSize[A](tree: Tree[A]): Int = {
    tree match {
      case EmptyTree                        =>
        1
      case NonEmptyTree(data, left, right)  =>
        treeSize(left) + treeSize(right)
    }
  }
  
  def SELECT(fields: String = "*") = {
    val parts = fields.split(",")
    // insert here
    // insert stream command to left of rootnode if empty
    //treeSize(rootNode)
    /*if(treeSize(rootNode) == 1){
      currentNode = currentNode.add(Select(parts.toList))
      //currentNode = rootNode.insert(node(Select(parts.toList)))
      //currentNode = insert(rootNode, node(Select(parts.toList)))
      println(s"TREE IS size 1 : ${node(Select(parts.toList))}")
      println(s"AFTER ADDING, what's returned: ${rootNode.add(Select(parts.toList))}")
    }*/

    currentNode = currentNode.add(Select(parts.toList))
    println(s"current TREE state : ${currentNode}")
    self
  }

  def INSERT(stream: String) = {
    currentNode = currentNode.add(Insert(stream))
    println(s"current TREE state : ${currentNode}")
    self
  }

  def FROM(clz: String) = {
    //TODO: ensure is preceded by Select
    //TODO: would be nice to use filter or map: currentNode.lastNode.filter
    def throwError = {
      throw new IllegalArgumentException("From should be preceeded by Select")
    }
    val lastNode = currentNode.lastNode
    println(s"LASTNODE: $lastNode")
    lastNode match {
      case EmptyTree => throwError
      case NonEmptyTree(d, l, r) => if(d.isInstanceOf[Select]) currentNode = currentNode.add(From(clz)) else throwError
    }
    self
  }

  def createEpl = {
    //val p = new Parser[Tree[T]] {}
    val parsed = parse(currentNode)
    eplString = parsed.mkString
    println(s"EPPL STRING: ${eplString}")
  }
}

object QueryDSL{
  def apply(): QueryDSL = new QueryDSL()
}
