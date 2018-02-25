import akka.actor.{ActorSystem, PoisonPill, Props}
import gui.SolverStage
import io.Parser._
import actors.ApplicationActor


import scalafx.application.JFXApp
import scalafx.Includes._

object SolverApp extends JFXApp {

  val myStage = new SolverStage(s"Solver: ${parameters.raw(0)}")
  val actorSystem = ActorSystem("solver-system")

  val appActor = actorSystem.actorOf(
    ApplicationActor.props(myStage, parameters.raw.toArray).withDispatcher("scalafx-dispatcher"),
    "app-actor"
  )


  myStage.onCloseRequest = handle {
    println("keke close")
    actorSystem.terminate()
  }

  stage = myStage

}
