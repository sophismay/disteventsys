package de.tud.disteventsys.dsl

import de.tud.disteventsys.actor.ActorCreator
import de.tud.disteventsys.event.Event._
import de.tud.disteventsys.dsl.QueryAST.{From, Select}
import de.tud.disteventsys.esper.{EsperStream, Statement}
import de.tud.disteventsys.common._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

import scala.concurrent.duration.Duration


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
  case class FromStream(es: EsperStream[_])              extends Operator
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


class QueryDSL extends Parser[Tree[Any]] with ActorCreator {
  self =>

  abstract class HandleParam[A]{
    def handle(gen: A): Unit
    def checkLastNodeBeforeAdd(clz: String) = {
      val lastNode = currentNode.lastNode
      println(s"LASTNODE: $lastNode")
      lastNode match {
        case EmptyTree => throwArgumentError
        case NonEmptyTree(d, l, r) => if(d.isInstanceOf[Select]) addToNode(From(clz)) else throwArgumentError
      }
    }
  }

  implicit object GeneratorHandleParam extends HandleParam[Generator[String]]{
    def handle(gen: Generator[String]): Unit = {
      gen match {
        case BuyGenerator(clz)    =>
          checkLastNodeBeforeAdd(clz)
        case PriceGenerator(clz)  =>
          checkLastNodeBeforeAdd(clz)
        case SellGenerator(clz)   =>
          checkLastNodeBeforeAdd(clz)
        //case EsperStreamGenerator(es) =>
        //TODO: actor dependency stuff
      }
    }
  }

  implicit object StreamHandleParam extends HandleParam[EsperStream.type]{
    def handle(stream: EsperStream.type) = {

    }
  }

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
  // to retraverse tree when Stream case encountered
  private def retraverse = {
    currentNode match {
      case NonEmptyTree(d, l, r) =>

      case EmptyTree    =>
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

  //def FROM[T](gen: Generator[T])(implicit hp: HandleParam[Generator[T]]) = {
  def FROM[T](gen: Any) = {
    //TODO: ensure is preceded by Select
    //TODO: would be nice to use filter or map: currentNode.lastNode.filter

    //hp.handle(gen)
    def checkLastNodeBeforeAdd(clz: String) = {
      val lastNode = currentNode.lastNode
      println(s"LASTNODE: $lastNode")
      lastNode match {
        case EmptyTree => throwArgumentError
        case NonEmptyTree(d, l, r) => if(d.isInstanceOf[Select]) addToNode(From(clz)) else throwArgumentError
      }
    }

    def checkLastNodeBeforeAddToStream(stream: EsperStream[_]) = {
      val lastNode = currentNode.lastNode
      lastNode match {
        case EmptyTree => throwArgumentError
        case NonEmptyTree(d, l, r) => if(d.isInstanceOf[Select]) addToNode(FromStream(stream)) else throwArgumentError
      }
    }
    /*println(s"GEN IS INSTANCE OF ESPERSTREAM: ${gen.isInstanceOf[EsperStream[Operator]]}")
    println(s"GEN IS INSTANCE OF ESPERSTREAM TYPE:  ${gen.isInstanceOf[EsperStream.type]}")
    println(s"GEN IS INSTANCE OF GENERATOR: ${gen.isInstanceOf[Generator[String]]}")*/

    if(gen.isInstanceOf[Generator[String]]){
      println(s"IS INSTANCE OF: $gen")
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
    }
    if(gen.isInstanceOf[EsperStream[_]]){
      println(s"INSTANCE OF ESPERSTREAM: $gen")
      gen match {
        case EsperStream(stream) =>
          //TODO: get node tree from stream and traverse current node to left side of stream tree
          // TODO: preempt after fromStream encountered
          createStreamFromStream(Array(stream))
          //val tree = stream.getTree
          //println(s"CURRENT NODE BEFORE RETRAVERSING: $currentNode")
          //checkLastNodeBeforeAddToStream(EsperStream(stream))
        /*case EsperStream(actor, esb, node) =>
          checkLastNodeBeforeAddToStream(EsperStream(actor, esb, node))*/
      }
    }



    self
  }

  /*def FROM[A](stream: A)(implicit hp: HandleParam[A]) = {

  }*/
  private def createStreamFromStream(streams: Array[de.tud.disteventsys.esper.Stream[_]]) = {
    //parseWithFields
    val parsedStringBuilder = createEpl
    processWithStreams(parsedStringBuilder, streams)
  }

  def createStream = {
    val parsedStringBuilder = createEpl
    val stream = processEpl(parsedStringBuilder)
    // reset tree and eplString
    currentNode = EmptyTree
    eplString = ""
    EsperStream(stream)
  }

  private def processEpl(sb: StringBuilder) = {
    // TODO: better way to handle future
    val streamFuture = process(sb, currentNode)
    val stream = Await.result(streamFuture, Duration.Inf)
    println(s"BEFORE RESETTING: ${sb.mkString}")
    stream
  }

  private def createEpl = {
    parse(currentNode)
    //eplString = parsed.mkString
    /*for {
      stream <- streamFuture
    } yield EsperStream(stream)*/
    //println(s"EPL STRING: ${eplString}")
    //println(s"EXPLODING EPLSTRING: ${eplString.split(' ').foreach(f=>println(s"$f : ah"))}")

    // return Esper Stream representation
    //EsperStream(optionActor, parsedStringBuilder, currentNode)
  }
}

object QueryDSL{
  def apply(): QueryDSL = new QueryDSL()
}
