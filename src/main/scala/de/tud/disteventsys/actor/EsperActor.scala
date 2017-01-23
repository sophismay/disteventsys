package de.tud.disteventsys.actor

import akka.actor.{Actor, ActorRef, Props}
import de.tud.disteventsys.esper.EsperEngine

/**
  * Created by ms on 18.11.16.
  */

object EsperActor{
  // to start processing events by esper engine
  case object StartProcessing

  // register actor class with esper engine
  case class RegisterEventType(name: String, clz: Class[_ <: Any])

  // considering case without listener too
  //case class DeployStatement(epl: String, listener: Option[ActorRef])
  case class DeployStatement(eplStatement: String, name: String)

  case class CreateActor(clz: String)

  case class ReceiveCreatedActor(actor: ActorRef)

  //case class Deploy(eplStatement: String, name: String)
}


class EsperActor extends Actor with EsperEngine{
  import EsperActor._

  private var createdActors: List[ActorRef] = List.empty

  def receive: Receive = {
    //
    case RegisterEventType(name, clz)   =>
      esperConfig.addEventType(name, clz.getName)

    /*case DeployStatement(epl, listener) =>
      println(s"INSIDE DEPLOY STATEMENT: ${listener}")
      createEPL(epl)(evt => listener map ( l => l ! evt))*/

    case StartProcessing                =>
      context.become(dispatchingToEsper)

    case CreateActor(clz)       =>
      clz match {
        case "Buy"  =>
          val actor = context.actorOf(Props(classOf[BuyerActor]), "Buy")
          createdActors = createdActors :+ actor
          println(s"ACTOR CREATED: ${actor}")
        case "Sell" =>
          createdActors = createdActors :+ context.actorOf(Props(classOf[SellerActor]), "Sell")
        case "Price" =>
          createdActors = createdActors :+ context.actorOf(Props(classOf[PriceActor]), "Price")
      }
    case DeployStatement(eplStatement, name) =>
      println(s"CASE DEPLOY: $name")
      val actor = {for {
        actor <- createdActors
        if(actor.path.name == name)
      } yield actor}.head
      println(s"CASE DEPLOY: $actor")
      createEPL(eplStatement)(evt => actor ! evt)
  }

  private def dispatchingToEsper():Receive = {
    case evt@_ => epRuntime.sendEvent(evt)
  }
}
