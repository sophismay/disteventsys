/**
  * Created by ms on 16.11.16.
  */
sealed abstract class Expr
final case class StringExpr(value: String)  extends Expr
final case class NumberExpr(value: Double)  extends Expr
final case class SymbolExpr(sym: String)    extends Expr
final case class CommandExpr(comm: String)  extends Expr