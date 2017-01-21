package de.tud.disteventsys.esper

import akka.actor.ActorRef
import de.tud.disteventsys.actor_classes.StreamReferenceGenerator
import de.tud.disteventsys.dsl.Tree

/**
  * Created by ms on 02.01.17.
  */
// a general representation of the actor, and it's epl statement
case class EsperStream[A](actor: Option[ActorRef], eplStringBuilder: StringBuilder, node: Tree[A]) extends Statement{
  initStatement(eplStringBuilder)
  private val ref = StreamReferenceGenerator.generateReference

  def getEplString: String = eplString
  def getStreamReference: String = ref
}
