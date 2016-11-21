/**
  * Created by ms on 17.11.16.
  */

abstract class Connection{
  def create: Unit
  def close: Unit
}

class EsperConnection extends Connection with EsperEngine{
  //private lazy val connection = _

  def create = {

  }
  def close = {

  }
}

object EsperConnection{
  def apply: EsperConnection = new EsperConnection()
}
