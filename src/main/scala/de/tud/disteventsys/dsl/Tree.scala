package de.tud.disteventsys.dsl

/**
  * Created by ms on 03.12.16.
  */
sealed abstract class Tree[+T] {
  def isEmpty: Boolean
  def add[A >: T](data: A): Tree[A]
  def toList: List[Tree[T]]
  def lastNode: Tree[T]
}

case object EmptyTree extends Tree[Nothing]{
  def isEmpty = true
  def add[A](data: A) = NonEmptyTree(data, EmptyTree, EmptyTree)
  def toList = Nil
  def lastNode = this
}

case class NonEmptyTree[+T](data: T, left: Tree[T], right: Tree[T]) extends Tree[T]{
  def isEmpty = false
  def add[A >: T](d: A): Tree[A] = {
    NonEmptyTree(data, left, right.add(d))
  }
  def toList = {
    List(NonEmptyTree(data, left, right)) ++ right.toList
  }

  def lastNode = {
    right match {
      case EmptyTree              => this
      case NonEmptyTree(d, l, r)  => if(!r.isEmpty) r.lastNode else right
    }
  }
}

object Tree{
  def empty = EmptyTree
  def node[T](data: T, left: Tree[T] = empty, right: Tree[T] = empty) = {
    NonEmptyTree(data, left, right)
  }
}