package actors

import actors.Messages.Solver.{InfoUpdateOnTotalGain, Init}
import akka.actor.SupervisorStrategy.{Escalate, Stop}
import logic.Domain.Situation
import akka.actor._
import logic.Context

import scala.concurrent.duration._

class SolverActor(situation: Situation) extends Actor {

  val system = new System(Context(situation))

  val systemActor = context.actorOf(SystemActor.props(system), "solver-system")

  val endpointActors =
    Array.tabulate(situation.endpoints.length){ endpointId =>
      context.actorOf(EndpointActor.props(system, endpointId), s"endpoint-$endpointId")
    }

  val cacheServerActors =
    Array.tabulate(situation.cacheServersN){ cacheServerId =>
      context.actorOf(CacheServerActor.props(system, cacheServerId), s"cacheServer-$cacheServerId")
    }

  system.systemActor = systemActor
  system.endpointActors = endpointActors
  system.cacheServerActors = cacheServerActors

  systemActor ! Init

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 0, withinTimeRange = 1 minute, loggingEnabled = true) {
      //TODO: DEAL WITH IT
      case _: java.util.NoSuchElementException => Stop
    }

  override def receive: Receive = {

    case (x @ InfoUpdateOnTotalGain(_)) => context.parent ! x
    case x => println(s"UNEXPECTED MESSAGE: $x")
  }

}

object SolverActor{

  def props(situation: Situation) = Props(classOf[SolverActor], situation)

}
