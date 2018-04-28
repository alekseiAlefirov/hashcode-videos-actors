package actors

import java.rmi.UnexpectedException

import actors.Messages.Solver._
import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import logic.Domain.Id

import scala.annotation.tailrec


class CacheServerActor(system: System, cacheServerId: Int) extends Actor with ActorLogging{

  import system.context._

  //videoId x (endpointId x gain)
  var allocationsAndRequests = Map.empty[Id, Map[Id, Int]]
  //videoId x (moreProfitableVideoId X (endpoint, promisedGain))
  //var bigAndNotSoProfitable = Map.empty[Id, mutable.Map[Id, mutable.Map[Id, Int]]]

  def emptySpace() = situation.cacheServerCapacity - weight(allocationsAndRequests.keys)
  def gainOfContainedVideo(video: Id) = allocationsAndRequests(video).values.sum

  override def receive: Receive = {

    case Application(initiatorId: Id, video, gains) =>

      log.debug(s"Applicated for video $video sized ${getVideoSize(video)} with gain of ${gains.values.sum}")

      def answerOnApplication(positive: Boolean) = {
        gains.keys.foreach(
          system.endpointActors(_) ! ApplicationAnswer(initiatorId, video, cacheServerId, positive)
        )
      }

      if (allocationsAndRequests.contains(video)) {
        log.debug(s"Applicated on already contained video $video")
        allocationsAndRequests = allocationsAndRequests.updated(video, gains)
        answerOnApplication(positive = true)
      } else {
        val emptySpaceBefore = emptySpace()
        decideToPutNewVideo(getVideoSize(video), gains.values.sum) match {
          case Some(toDelete) => {
            log.debug(s"Decided to put video $video and delete (${toDelete.mkString(",")})")
            toDelete.foreach { videoToDelete =>
              val interesands = allocationsAndRequests(videoToDelete).keys
              interesands.foreach(system.endpointActors(_) ! VideoRemoved(videoToDelete, cacheServerId))
              allocationsAndRequests = allocationsAndRequests - videoToDelete
              log.debug(s"Removed video ($videoToDelete, ${getVideoSize(videoToDelete)}), empty space: ${emptySpace()}, Current: ${allocationsAndRequests.keys.mkString(", ")}")
            }
            allocationsAndRequests = allocationsAndRequests.updated(video, gains)
            if(toDelete.isEmpty && emptySpace() == emptySpaceBefore) {
              log.error(s"Something strange, emptySpace = ${emptySpace()}, allocationsAndRequests = ${allocationsAndRequests}")
              context.system.terminate()
            }
            log.debug(s"Added video ($video, ${getVideoSize(video)}), empty space: ${emptySpace()}, Current: ${allocationsAndRequests.keys.mkString(", ")}")
            answerOnApplication(positive = true)
            if (emptySpace() < 0) {
              /////////////////////////DEBUG
              //todo: make it better
              log.error(s"Overflow!!" +
                s"Current: ${allocationsAndRequests.keys.mkString(", ")}, Empty space: ${emptySpace()}," +
                s" added: $video, ${getVideoSize(video)}, removed: ${toDelete.map(x => (x, getVideoSize(x))).mkString(", ")}")
              context.system.terminate()
            }
          }

          //TODO: it's a simple greedy algo,
          //implement logic when 2 videos chosen instead of one bringing less gain
          case None => answerOnApplication(positive = false)
        }
      }

    case GainUpdated(endpointId, video, gain) =>
      if(allocationsAndRequests.contains(video)) {
        log.debug(s"Update gain for video $video")
        allocationsAndRequests = allocationsAndRequests.updated(video, allocationsAndRequests(video).updated(endpointId, gain))
      }

  }

  /**
    *
    * @return what to remove
    */
  def decideToPutNewVideo(sizeToPut: Int, gain: Int): Option[Seq[Id]] = {

    def helper(sizeToPut: Int, gain: Int, containedVideos: List[Id]): Option[Seq[Id]] = {
      if(sizeToPut <= 0) {
        Some(Seq.empty)
      } else containedVideos match {
        case Nil => None
        case v::vs =>
          val vGain = gainOfContainedVideo(v)
          val vSize = getVideoSize(v)
          if(vGain > gain || (vGain == gain && vSize < sizeToPut)) {
            helper(sizeToPut, gain, vs)
          } else if (vGain == gain) {
            //&& vSize >= sizeToPut
            Some(Seq(v))
          } else {
            //if (vGain < gain)
            helper(sizeToPut - vSize, gain - vGain, vs)
              .map(v +: _).orElse(helper(sizeToPut, gain, vs))
          }
      }
    }

    helper(sizeToPut - emptySpace(), gain, allocationsAndRequests.keys.toList)
  }

  def gainPerSpace(video: Id, gainOpt: Option[Int] = None): Double = {
    val gain = gainOpt.getOrElse(allocationsAndRequests(video).values.sum)
    gain.toDouble / getVideoSize(video)
  }
}

object CacheServerActor {

  def props(system: System, cacheServerId: Int) = Props(classOf[CacheServerActor], system, cacheServerId)

}
