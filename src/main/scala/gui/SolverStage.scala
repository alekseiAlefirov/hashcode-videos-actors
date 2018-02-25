package gui
import actors.ApplicationActor
import akka.actor.{ActorSystem, Props}
import actors.Messages.Application

import scalafx.Includes.handle
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.control.{Button, TextField}
import scalafx.scene.layout.HBox


class SolverStage(_title: String) extends JFXApp.PrimaryStage {

  title.value = _title
  width = 600
  height = 450

  var onPauseButtonClicked : Unit => Unit = { _ => () }
  var onPopulationCountUBClicked : Int => Unit = { _ => () }
  var onInvadeButtonClicked : Unit => Unit = {_ => () }

  val pauseButton = new Button("Pause"){
    onMouseClicked = handle { onPauseButtonClicked() }
  }

  val populationCountBox = new TextField
  val populationCountUpdateButton = new Button("Update"){
    onMouseClicked = handle { onPopulationCountUBClicked( populationCountBox.text.value.toInt ) }
  }

  val invadeButton = new Button("Invade!"){
    onMouseClicked = handle { onInvadeButtonClicked() }
  }

  scene = new Scene {
    //fill = LightGreen
    content = new HBox {
      children = Seq(
        pauseButton,
        populationCountBox,
        populationCountUpdateButton,
        invadeButton
      )
    }
  }

}
