package gui

import scalafx.Includes.handle
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, TextArea, TextField}
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox


class SolverStage(_title: String) extends JFXApp.PrimaryStage {

  title.value = _title
  width = 600
  height = 450

  //var onPauseButtonClicked:        Unit => Unit = { _ => () }

  /*val pauseButton = new Button("Pause"){
    onMouseClicked = handle { onPauseButtonClicked() }
  }*/

  val highscoreLabel = new Label("Aprx. highscore: ") {
    def updateHighScore(value: Int) = {
      this.text_=(s"Aprx. highscore: $value")
    }
  }

  scene = new Scene {
    //fill = LightGreen
    content = new VBox {
      children = Seq (
        highscoreLabel
      )
    }
  }

}
