package de.tud.disteventsys.dsl

/**
  * Created by ms on 21.11.16.
  */

trait QueryAST {
  type Stream
  type Schema = Vector[String]

  // Parent Operator for Esper Stream
  sealed abstract class ParentOperator
  // clz: class to insert into
  case class INSERT(clz: String) extends ParentOperator

  // Operators such as select
  sealed abstract class Operator
  case class Select(parent: ParentOperator, fields: Field*) extends Operator
  case class From(parent: Operator, clz: String)            extends Operator
  case class Where(parent: Operator, expr: Expr)            extends Operator

  // Expressions/ Filters
  abstract sealed class Expr
  case class Literal(value: Any)         extends Expr
  case class eq(left: Expr, right: Expr) extends Expr

  // References
  sealed abstract class Ref
  case class Field(name: String) extends Ref
  case class Value(value: Any)   extends Ref

  // smart constructors

}


trait QueryDSL {
  private var eplString: String = _

  def SELECT(fields: String = "*") = {
    val parts = fields.split(",")
  }
}
