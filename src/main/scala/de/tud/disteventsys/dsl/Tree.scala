package de.tud.disteventsys.dsl

/**
  * Created by ms on 03.12.16.
  */
sealed abstract class Tree[+T] {
  def isEmpty: Boolean
  def size: Int
  def add[A >: T](data: A): Tree[A]
}

case object EmptyTree extends Tree[Nothing]{
  def isEmpty = true
  def size = 0
  def add[A](data: A) = NonEmptyTree(data, EmptyTree, EmptyTree)
}

case class NonEmptyTree[+T](data: T, left: Tree[T], right: Tree[T]) extends Tree[T]{
  def isEmpty = false
  def size = {
    ???
  }
  def add[A >: T](d: A): Tree[A] = {
    NonEmptyTree(data, left, right.add(d))
  }
}

object Tree{
  def empty = EmptyTree
  def node[T](data: T, left: Tree[T] = empty, right: Tree[T] = empty) = {
    NonEmptyTree(data, left, right)
  }
}