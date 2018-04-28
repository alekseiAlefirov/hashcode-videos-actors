package actors

import actors.Messages.Solver._
import akka.actor.{Actor, ActorLogging, Props}

class SystemActor(system: System) extends Actor with ActorLogging{

  val gains: Array[Long] = system.situation.endpoints.map(_ => 0L)

  def totalGain() = (gains.sum * 1000 / system.situation.requestsN).toInt

  override def receive: Receive = {
    case Init =>
      log.info("system init")
      log.info(s"endpoint actors: ${system.endpointActors.map(_.path.name).mkString(", ")}")
      system.endpointActors.foreach(_ ! Work)

    case InfoUpdateOnGain(endpointId, gain) =>
      gains(endpointId) = gain
      context.parent ! InfoUpdateOnTotalGain(totalGain())
  }
}

object SystemActor {

  def props(system: System) = Props(classOf[SystemActor], system)

}
