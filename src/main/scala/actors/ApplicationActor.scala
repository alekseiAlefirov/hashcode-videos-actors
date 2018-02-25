package actors

import actors.Messages.Application.Invade
import actors.Messages.Solver.Work
import io.Parser.parseFile
import actors.Messages.{Application, Solver}
import akka.actor.{Actor, ActorRef, Props}
import gui.SolverStage
import logic.Domain.Situation
import logic.Solutions.Solution


class ApplicationActor(guiStage: SolverStage, args: Array[String]) extends Actor {

  var solver = ActorRef.noSender
  var paused = false
  var highscore = 0L
  var bestSolution = Solution(Array())

  override def preStart() = {

    val situation = parseFile(args(0))
    solver = context.actorOf(SolverActor.props(situation))
    solver ! Solver.Work
    initGui()

  }

  override def receive: Receive = {

    case Application.Pause =>
      guiStage.pauseButton.text = "Unpause"
      guiStage.onPauseButtonClicked = _ => self ! Application.Unpause
      paused = true

    case Application.Unpause =>
      guiStage.pauseButton.text = "Pause"
      guiStage.onPauseButtonClicked = _ => self ! Application.Pause
      paused = false
      solver ! Work

    case (x @ Application.UpdatePopulationCount(_)) => solver ! x
    case Invade => solver ! Invade

    case Solver.Iterated(updated) => {
      updated.foreach{
        case (newBestSolution, newHighscore) =>
          bestSolution = newBestSolution
          highscore = newHighscore
      }
      if(!paused) sender ! Solver.Work
    }

  }

  def initGui(): Unit = {
    guiStage.onPauseButtonClicked       = _ => self ! Application.Pause
    guiStage.onPopulationCountUBClicked =      self ! Application.UpdatePopulationCount(_)
    guiStage.onInvadeButtonClicked =      _ => self ! Application.Invade
  }

}

object ApplicationActor{

  def props(guiStage: SolverStage, args: Array[String]) = Props(classOf[ApplicationActor], guiStage, args)

}
