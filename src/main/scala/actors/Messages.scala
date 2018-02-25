package actors

import logic.Solutions.Solution

object Messages {

  abstract class Message

  object Application {

    case object Pause extends Message
    case object Unpause extends Message
    case class UpdatePopulationCount(count: Int) extends Message
    case object Invade extends Message
  }

  object Solver{

    case object Work extends Message
    case class Iterated(newBestSolution: Option[(Solution, Long)])

  }

}
