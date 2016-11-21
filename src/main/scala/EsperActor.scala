import akka.actor.{Actor, ActorRef}

/**
  * Created by ms on 18.11.16.
  */

object EsperActor{
  // to start processing events by esper engine
  case object StartProcessing

  // register actor class with esper engine
  case class RegisterEventType(name: String, clz: Class[_ <: Any])

  // considering case without listener too
  case class DeployStatement(epl: String, listener: Option[ActorRef])
}


class EsperActor extends Actor with EsperEngine{
  import EsperActor._

  def receive: Receive = {
    //
    case RegisterEventType(name, clz)   =>
      esperConfig.addEventType(name, clz.getName)

    case DeployStatement(epl, listener) =>
      createEPL(epl)(evt => listener map ( l => l ! evt))

    case StartProcessing                =>
      context.become(dispatchingToEsper)
  }

  private def dispatchingToEsper():Receive = {
    case evt@_ => epRuntime.sendEvent(evt)
  }
}
