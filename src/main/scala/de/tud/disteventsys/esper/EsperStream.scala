package de.tud.disteventsys.esper

import akka.actor.ActorRef
import de.tud.disteventsys.dsl.Tree

/**
  * Created by ms on 02.01.17.
  */
// a general representation of the actor, and it's epl statement
case class EsperStream[A](actor: ActorRef, eplString: String, node: Tree[A])
