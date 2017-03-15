package de.tud.disteventsys.dsl

import de.tud.disteventsys.actor.ActorCreator
import de.tud.disteventsys.esper.{EsperStream}
import de.tud.disteventsys.common._
import scala.concurrent.Await
import scala.concurrent.duration.Duration


/**
  * Created by ms on 21.11.16.
  */

object QueryAST {

  // Operators such as select
  sealed abstract class Operator
  case class Select(fields: List[String])                                        extends Operator
  case class Insert(stream: String)                                              extends Operator
  case class From(clz: String, extras: Option[Map[String, String]], gen: AnyRef) extends Operator
  case class FromStream(es: EsperStream[_])                                      extends Operator
  case class Where(clz: String, clause: String)                                  extends Operator

  // Expressions/ Filters
  abstract sealed class Expr
  case object NoExpr                        extends Expr
  case class Literal(value: String)         extends Expr
  case class Equal(left: Expr, right: Expr) extends Expr
}


class QueryDSL extends Parser[Tree[Any]] with ActorCreator {
  self =>

  import Tree._
  import QueryAST._

  private var eplString: String = _
  private val rootNode = empty
  // flag to check for dependence of dsl instance on stream
  private var dependsOnStream = false
  private var dependentStreams: Array[de.tud.disteventsys.esper.Stream[_]] = Array.empty
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
    gen match {
      case FieldsGenerator(fields)  =>
        addToNode(Select(fields.split(",").map{ f => f.trim }.toList))
      case _                        =>
        throwArgumentError
    }

    self
  }

  def INSERT[T](stream: Generator[T]) = {
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

    self
  }


  // find and get corresponding generator
  private def getPriceGenerator = {
    // get last node and return, that is, the event after FROM
    val lastNode = currentNode.lastNode
    val generator = lastNode match {
      case NonEmptyTree(clauseAST, left, right) =>
        clauseAST match {
          case c@From(_, _, _) =>
            val generator = c.gen
            generator match {
              case gen@PriceGenerator(_) =>
                //println(s"ACTUAL GEN: $gen")
                gen
            }
        }
      //case EmptyTree =>
    }
    println(s"LAST NODE FROM WHERE: $lastNode")
    generator
  }
  private def getSellGenerator = {
    val lastNode = currentNode.lastNode
    val generator = lastNode match {
      case NonEmptyTree(clauseAST, l, r) =>
        clauseAST match {
          case c@From(_, _, _) =>
            val genert = c.gen
            genert match {
              case gen@SellGenerator(_) =>
                gen
            }
        }
    }
    generator
  }
  private def getBuyGenerator = {
    val lastNode = currentNode.lastNode
    val generator = lastNode match {
      case NonEmptyTree(clauseAST, l, r) =>
        clauseAST match {
          case c@From(_, _, _) =>
            val genert = c.gen
            genert match {
              case gen@BuyGenerator(_) =>
                gen
            }
        }
    }
    generator
  }

  def WHERE(f: PriceGenerator => Unit) = {
    val generator = getPriceGenerator
    f(generator)
    addToNode(Where("Price", generator.clause))

    self
  }

  private def addToNode(o: Operator) = {
    currentNode = currentNode.add(o)
  }

  private def throwArgumentError = {
    throw new IllegalArgumentException("From should be preceeded by Select")
  }

  def FROM[T](gen: Any) = {
    // check if last node in tree (SELECT) before adding FROM
    def checkLastNodeBeforeAdd(clz: String, extras: Option[Map[String, String]], gen: AnyRef) = {
      val lastNode = currentNode.lastNode
      println(s"LASTNODE: $lastNode")
      lastNode match {
        case EmptyTree => throwArgumentError
        case NonEmptyTree(d, l, r) => if(d.isInstanceOf[Select]) addToNode(From(clz, extras, gen)) else throwArgumentError
      }
    }

    def checkLastNodeBeforeAddToStream(stream: EsperStream[_]) = {
      val lastNode = currentNode.lastNode
      lastNode match {
        case EmptyTree => throwArgumentError
        case NonEmptyTree(d, l, r) => if(d.isInstanceOf[Select]) addToNode(FromStream(stream)) else throwArgumentError
      }
    }

    if(gen.isInstanceOf[Generator[String]]){
      println(s"IS INSTANCE OF: $gen")
      gen match {
        case bg@BuyGenerator(clz)    =>
          checkLastNodeBeforeAdd(clz, None, bg)
        case pg@PriceGenerator(clz)  =>
          if(pg.hasUniqueField) {
            //println(s"PG has Unique field: ${pg.getUniqueField}")
            checkLastNodeBeforeAdd(clz, Some(Map("unique" -> pg.getUniqueField)), pg)
          } else {
            checkLastNodeBeforeAdd(clz, None, pg)
          }

        case sg@SellGenerator(clz)   =>
          checkLastNodeBeforeAdd(clz, None, sg)
        case EsperStreamGenerator(es) =>
      }
    }
    if(gen.isInstanceOf[EsperStream[_]]){
      gen match {
        case EsperStream(stream) =>
          // preempt after fromStream encountered
          // flag to depict dependence on stream
          dependsOnStream = true
          dependentStreams = dependentStreams :+ stream
      }
    }

    self
  }

  private def resetVariables = {
    currentNode = EmptyTree
    eplString = ""
    dependsOnStream = false
    dependentStreams = Array.empty
  }

  private def createStreamFromStream = {
    val parsedStringBuilder = createEpl
    val stream = processWithStreams(parsedStringBuilder, dependentStreams, currentNode)

    resetVariables
    EsperStream(stream)
  }

  def createStream: EsperStream[_] = {
    if(!dependsOnStream) return createStreamFromStatement else return createStreamFromStream
  }

  // create stream representation from query
  private def createStreamFromStatement = {
    val parsedStringBuilder = createEpl
    val stream = processEpl(parsedStringBuilder)
    // reset tree and eplString
    resetVariables
    EsperStream(stream)
  }

  private def processEpl(sb: StringBuilder) = {
    val streamFuture = process(sb, currentNode)
    val stream = Await.result(streamFuture, Duration.Inf)

    stream
  }

  private def createEpl = {
    parse(currentNode)
  }
}

object QueryDSL{
  def apply(): QueryDSL = new QueryDSL()
}
