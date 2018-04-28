package actors

import actors.Messages.Solver.InfoUpdateOnTotalGain
import io.Parser.parseFile
import actors.Messages._
import akka.actor.{Actor, ActorRef, Props}
import gui.SolverStage
import logic.Solutions.Solution


class UiActor(guiStage: SolverStage, args: Array[String]) extends Actor {

  var solver = ActorRef.noSender
  var paused = false
  var highscore = 0L
  var bestSolution = Solution(Array())

  override def preStart() = {

    val situation = parseFile(args(0))
    solver = context.actorOf(SolverActor.props(situation))
    initGui()

  }

  override def receive: Receive = {

    /*case Application.Pause =>
      guiStage.pauseButton.text = "Unpause"
      guiStage.onPauseButtonClicked = _ => self ! Application.Unpause
      paused = true

    case Application.Unpause =>
      guiStage.pauseButton.text = "Pause"
      guiStage.onPauseButtonClicked = _ => self ! Application.Pause
      paused = false
      solver ! Work*/

    case InfoUpdateOnTotalGain(gain) =>
      guiStage.highscoreLabel.updateHighScore(gain)
    case x => println(s"UNEXPECTED MESSAGE: $x")


  }

  def initGui(): Unit = {
    /*guiStage.onPauseButtonClicked       =  _ => self ! Application.Pause
    guiStage.onPopulationCountUBClicked =       self ! Application.UpdatePopulationCount(_)
    guiStage.onInvadeButtonClicked =       _ => self ! Application.Invade
    guiStage.onCreateLegendButtonClicked = _ => self ! Application.CreateLegend*/
  }

}

object UiActor{

  def props(guiStage: SolverStage, args: Array[String]) = Props(classOf[UiActor], guiStage, args)

}
