package de.tud.disteventsys.dsl

import de.tud.disteventsys.actor.ActorCreator
import de.tud.disteventsys.actor_classes._
import de.tud.disteventsys.esper.EsperStream

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


class QueryDSL extends Parser[Tree[Any]] with ActorCreator{
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
  
  def SELECT[T](gen: Generator[T]) = {
    //val parts = fields.split(",")
    // insert here
    // insert stream command to left of rootnode if empty
    // fields: List[String] = List("*")
    gen match {
      case FieldsGenerator(fields)  =>
        addToNode(Select(fields.split(",").map{ f => f.trim }.toList))
      case _                        =>
        throwArgumentError
    }

    /*currentNode = currentNode.add(Select(fields))
    println(s"current TREE state : ${currentNode}")*/

    self
  }

  def INSERT[T](stream: Generator[T]) = {
    //TODO: would be nice to use map or flatmap

    stream match {
      case BuyGenerator(clz)         =>
        currentNode = currentNode.add(Insert(clz))
        println(s"current TREE state : ${currentNode}")
      case PriceGenerator(clz)       =>
        currentNode = currentNode.add(Insert(clz))
        println(s"current TREE state : ${currentNode}")
      case SellGenerator(clz)        =>
        currentNode = currentNode.add(Insert(clz))
        println(s"current TREE state : ${currentNode}")
      case _                         =>
        throw new IllegalArgumentException("You can't Insert into an existing stream")
    }
    //currentNode = currentNode.add(Insert(stream))

    self
  }

  private def addToNode(o: Operator) = {
    currentNode = currentNode.add(o)
  }

  private def throwArgumentError = {
    throw new IllegalArgumentException("From should be preceeded by Select")
  }

  def FROM[T](gen: Generator[T]) = {
    //TODO: ensure is preceded by Select
    //TODO: would be nice to use filter or map: currentNode.lastNode.filter

    def checkLastNodeBeforeAdd(clz: String) = {
      val lastNode = currentNode.lastNode
      println(s"LASTNODE: $lastNode")
      lastNode match {
        case EmptyTree => throwArgumentError
        case NonEmptyTree(d, l, r) => if(d.isInstanceOf[Select]) addToNode(From(clz)) else throwArgumentError
      }
    }

    gen match {
      case BuyGenerator(clz)    =>
        checkLastNodeBeforeAdd(clz)
      case PriceGenerator(clz)  =>
        checkLastNodeBeforeAdd(clz)
      case SellGenerator(clz)   =>
        checkLastNodeBeforeAdd(clz)
      case EsperStreamGenerator(es) =>
        //TODO: actor dependency stuff
    }

    self
  }

  def createStream = {
    createEpl
  }

  private def createEpl = {
    val parsed = parse(currentNode)
    eplString = parsed.mkString
    val actor = process(eplString)
    println(s"EPL STRING: ${eplString}")
    //println(s"EXPLODING EPLSTRING: ${eplString.split(' ').foreach(f=>println(s"$f : ah"))}")

    // return Esper Stream representation
    EsperStream(actor, eplString, currentNode)
  }
}

object QueryDSL{
  def apply(): QueryDSL = new QueryDSL()
}
