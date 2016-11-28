package de.tud.disteventsys.dsl

import sun.font.TrueTypeFont

import scala.collection.immutable.RedBlackTree.Tree

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
  case class Select(parent: ParentOperator, fields: List[String]) extends Operator
  case class From(parent: Operator, clz: String)            extends Operator
  case class Where(parent: Operator, expr: Expr)            extends Operator

  // Expressions/ Filters
  abstract sealed class Expr
  case object NoExpr                     extends Expr
  case class Literal(value: Any)         extends Expr
  case class eq(left: Expr, right: Expr) extends Expr

  // References
  sealed abstract class Ref
  case class Field(name: String) extends Ref
  case class Value(value: Any)   extends Ref

  // smart constructors

}

import QueryAST._

sealed abstract class Tree[+A]{
  def isEmpty: Boolean
  def insert[A](data: A): BinaryTree[A]
  //def size: Int
  //def isParent: Boolean
  def entries: Iterable[A]
}

sealed abstract class BinaryTree[+A] extends Tree[A]{
  //def insert[A]: BinaryTree[A]
}

case object EmptyTree extends BinaryTree[Nothing]{
  override def isEmpty: Boolean = true
  // TODO: check if first time adding(stream specific command on top)
  def insert[A](data: A): BinaryTree[A] = NonEmptyTree(data, EmptyTree, EmptyTree)
  //override def size: Int = 0
  import scala.collection.immutable.Nil
  override def entries: Iterable[Nil.type] = Nil
}
//Tuple2[Operator, Expr]
case class NonEmptyTree[A](data: A, left: BinaryTree[A], right: BinaryTree[A]) extends BinaryTree[A]{
  override def isEmpty: Boolean = false
  // TODO: check if first time adding(stream specific command on top)
  def insert[A](data: A): BinaryTree[A] = {
    NonEmptyTree(data, EmptyTree, EmptyTree)
  }
  def entries: Iterable[A] = ???
}

object Tree{
  def empty[A]: BinaryTree[A] = EmptyTree
  def node[A](data: A, left: BinaryTree[A] = empty, right: BinaryTree[A] = empty): NonEmptyTree[A] = {
    NonEmptyTree(data, left, right)
  }
}

//class EsperActorDSL[T] extends Tree[A, B]() with QueryAST{
//  type This = EsperActorDSL[T]
//}


class QueryDSL {
  self =>
  
  private var eplString: String = _
  import Tree._
  private val tree = empty

  def treeSize[A](tree: Tree[A]): Int = {
    tree match {
      case EmptyTree                        =>
        1
      case NonEmptyTree(data, left, right)  =>
        treeSize(left) + treeSize(right)
    }
  }
  
  def SELECT(fields: String = "*") = {
    val parts = fields match {
      case f: String =>
        if(f.isEmpty)
          Nil
      case _         =>

    }
    //val parts = fields.split(",")
    self
  }

  def INSERT(stream: String) = {

  }
}

object QueryDSL{
  def apply: QueryDSL = new QueryDSL()
}
