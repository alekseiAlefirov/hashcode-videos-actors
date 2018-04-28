package actors

import akka.actor.ActorRef
import logic.Domain.{Id, Request}
import logic.Solutions.Solution

object Messages {

  abstract class Message

  object Ui {

    //case object Pause extends Message
    //case object Unpause extends Message

  }

  object Solver{

    case object Init
    case object Work
    case class CallForApplication(initiatorId: Id, video: Id, server: Id)

    //answer is number of requests
    case class CallForApplicationAnswer(originalCall: CallForApplication, answererId: Id, requests: Int)
    case class Application(initiatorId: Id, video: Id, gains: Map[Id, Int])

    case class ApplicationAnswer(initiatorId: Id, video: Id, cacheServer: Id, positive: Boolean)
    case class GainUpdated(endpoint: Id, video: Id, gain: Int)
    case class VideoRemoved(video: Id, cacheServer:Id)
    case class InfoUpdateOnGain(endpoint: Id, gain: Long)
    case class InfoUpdateOnTotalGain(gain: Int)

  }

}
