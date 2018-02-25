package actors

import actors.Messages.Application
import actors.Messages.Application.{Invade, UpdatePopulationCount}
import actors.Messages.Solver.Work
import logic.Domain.Situation
import akka.actor._
import logic.Solver

class SolverActor(situation: Situation) extends Actor {

  val solver = new Solver(situation)
  println(s"Population count: ${solver.populationN}")

  override def receive: Receive = {
    case Work =>
      val updated = solver.iterate()
      if (updated) {
        val bestSolution = Solver.scoredSolution2Solution(solver.bestSolution)
        sender ! Messages.Solver.Iterated(Some(bestSolution, solver.bestSolution.score))
      }
      else {
        sender ! Messages.Solver.Iterated(None)
      }

    case UpdatePopulationCount(count) =>
      solver.changePopulationCount(count)

    case Invade => solver.invade()
  }

}

object SolverActor{

  def props(situation: Situation) = Props(classOf[SolverActor], situation)

}
