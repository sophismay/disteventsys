package de.tud.disteventsys.dsl

/**
  * Created by ms on 03.12.16.
  */
sealed abstract class Tree[+T] {
  def isEmpty: Boolean
  def size: Int
  def add[A >: T](data: A): Tree[A]
  def toList: List[Tree[T]]
}

case object EmptyTree extends Tree[Nothing]{
  def isEmpty = true
  def size = 0
  def add[A](data: A) = NonEmptyTree(data, EmptyTree, EmptyTree)
  def toList = Nil
}

case class NonEmptyTree[+T](data: T, left: Tree[T], right: Tree[T]) extends Tree[T]{
  def isEmpty = false
  def size = {
    ???
  }
  def add[A >: T](d: A): Tree[A] = {
    NonEmptyTree(data, left, right.add(d))
  }
  def toList = {
    List(NonEmptyTree(data, left, right)) ++ right.toList
  }

}

object Tree{
  def empty = EmptyTree
  def node[T](data: T, left: Tree[T] = empty, right: Tree[T] = empty) = {
    NonEmptyTree(data, left, right)
  }

  def toList[T](tree: Tree[T]): List[Tree[T]] = {
    tree match {
      case EmptyTree =>
        Nil
      case NonEmptyTree(d, l , r) =>
        List(NonEmptyTree(d, l, r)) ++ toList(r)
    }
  }
}