package actors

import actors.Messages.Solver._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import logic.Domain.{Id, Latency, Request}
import logic.RandomHelpers

import scala.util.Random

class EndpointActor(system: System, endpointId: Int) extends Actor with ActorLogging {

  val endpoint = system.situation.endpoints(endpointId)
  import endpoint.{dataCenterLatency, connections}
  val requestsByVideo: Map[Id, Int] =
    system.requestsByEndpoint(endpointId).groupBy(_.videoId).mapValues{
      requests =>
        requests.foldLeft(0){ case (total, Request(_, _, times)) => total + times }
    }

  var videoAllocations : Map[Id, Seq[Id]] = requestsByVideo.mapValues(_ => Seq.empty[Id])

  var notConsideredVideos: Set[Id] = requestsByVideo.keys.toSet
  var consideredVideos = Set.empty[Id]
  //videoId x (server x waited answers from endpoints x applicants x gains)
  var inProcessVideos = Map.empty[Id, (Id, Int, Map[Id, Int])]
  var inProcessByOther = Map.empty[Id, CallForApplicationAnswer]

  implicit val random = new Random()

  override def receive: Receive = {

    case Work => iterate()

    case (call @ CallForApplication(initiatorId, video, server)) =>

      if(requestsByVideo.contains(video)
        && !inProcessVideos.contains(video)
        && !inProcessByOther.contains(video)
        && currentLatency(video) >= connections(server)
      ) {
        val requests =
          if(currentLatency(video) > connections(server)) {
            requestsByVideo(video)
          } else {
            //equals
            val alreadyAllocatedServers = videoAllocations(video).takeWhile(connections(_) == currentLatency(video))
            requestsByVideo(video) / (alreadyAllocatedServers.length + 1)
          }
        val answer = CallForApplicationAnswer(call, endpointId, requests)
        inProcessByOther = inProcessByOther.updated(video, answer)
        sender ! answer
      } else {
        if (inProcessVideos.contains(video)){
          val (reservedServer, _, gains) = inProcessVideos(video)
          if(server == reservedServer) {
            sender() ! CallForApplicationAnswer(call, endpointId, gains(endpointId))
          }
        } else if (inProcessByOther.contains(video) && inProcessByOther(video).originalCall.server == server) {
          sender() ! CallForApplicationAnswer(call, endpointId, inProcessByOther(video).requests)
        } else {
          sender ! CallForApplicationAnswer(call, endpointId, 0)
        }
      }

    case CallForApplicationAnswer(CallForApplication(_, video, _), answererId, requests) =>
      val (server, oldAnswersRemain, oldGains) = inProcessVideos(video)
      val answersRemain = oldAnswersRemain - 1
      val gains = oldGains.updated(answererId, requests)
      inProcessVideos = inProcessVideos.updated(video, (server, answersRemain, gains))

      if(answersRemain == 0) {
        log.debug(s"Sending application to server $server for video $video")
        system.cacheServerActors(server) ! Application(endpointId, video, gains)
      }

    case ApplicationAnswer(initiatorId, video, cacheServer, positive) =>
      if(positive) {
        val oldAllocation = videoAllocations(video)
        val newAllocation = (cacheServer +: videoAllocations(video)).sortBy(connections(_))
        videoAllocations = videoAllocations.updated(video, newAllocation)

        if(oldAllocation.nonEmpty &&
          connections(cacheServer) == connections(newAllocation.head)) {
          //video is now delivered via new connection, previous connection should be updated on gain loss
          val (gaining, nonGaining) =
            oldAllocation
              .span(connections(_) == connections(cacheServer))
          val gainPerCacheServer = requestsByVideo(video) / (gaining.length + 1)//1 for new video
          gaining.filterNot(_ == cacheServer).foreach(system.cacheServerActors(_) ! GainUpdated(endpointId, video, gainPerCacheServer))
          nonGaining.foreach(system.cacheServerActors(_) ! GainUpdated(endpointId, video, 0))
        }
        sendInfoUpdateOnGain()
      }

      if(initiatorId == endpointId && inProcessVideos.contains(video)) {
        inProcessVideos = inProcessVideos - video
        consideredVideos = consideredVideos + video
      }
      if(inProcessByOther.contains(video) && inProcessByOther(video).originalCall.initiatorId == initiatorId) {
        inProcessByOther = inProcessByOther - video
      }

    case VideoRemoved(video: Id, cacheServer: Id) =>
      val newAllocation = videoAllocations(video).filterNot(_ == video)
      videoAllocations = videoAllocations.updated(video, newAllocation)
      if(newAllocation.nonEmpty && connections(newAllocation.head) >= connections(cacheServer)) {
        val gaining = newAllocation.takeWhile(connections(_) == connections(newAllocation.head))
        val gainPerCacheServer = requestsByVideo(video) / gaining.length
        gaining.foreach(system.cacheServerActors(_) ! GainUpdated(endpointId, video, gainPerCacheServer))
      }
      sendInfoUpdateOnGain()
  }

  def iterate(): Unit = {
    if(notConsideredVideos.isEmpty) {
      notConsideredVideos = consideredVideos
      consideredVideos = Set.empty
    }
    val toConsiderVideos = notConsideredVideos diff inProcessByOther.keys.toSet
    if(toConsiderVideos.nonEmpty) {
      val (video, _) = RandomHelpers.takeRandomAndRest(toConsiderVideos.toSeq)
      notConsideredVideos = notConsideredVideos - video
      val preferredServers = connections.filter(_._2 < currentLatency(video)).keys.toSeq
      if(preferredServers.nonEmpty) {
        val server = RandomHelpers.takeRandomAndRest(preferredServers)._1
        val ints = interesands(video, server).filterNot(_ == self)
        val initialGains = Map(endpointId -> requestsByVideo(video))
        inProcessVideos = inProcessVideos.updated(
          video,
          (server, ints.size, initialGains)
        )
        if(ints.nonEmpty) {
          val call = CallForApplication(endpointId, video, server)
          ints.foreach(_ ! call)
        } else {
          //log.debug(s"Sending application to server $server for video $video")
          system.cacheServerActors(server) ! Application(endpointId, video, initialGains)
        }
      } else {
        consideredVideos += video
      }
      self ! Work
    }
  }

  def sendInfoUpdateOnGain(): Unit = {
    val gain = videoAllocations.foldLeft(0L){
      case (gainAcc, (video, cacheServer::_)) =>
        gainAcc + (dataCenterLatency - connections(cacheServer)) * requestsByVideo(video)
      case (gainAcc, _ ) => gainAcc
    }

    log.debug(s"Sending gain update info $gain")
    system.systemActor ! InfoUpdateOnGain(endpointId, gain)
  }

  def interesands(video: Id, server: Id): Seq[ActorRef] = {
    system
      .requestsByVideo(video)
      .map(_.endpointId)
      .filter(system.situation.endpoints(_).connections.contains(server))
      .map(system.endpointActors(_))
  }

  def currentLatency(video: Id): Latency = {
    videoAllocations(video).headOption.getOrElse(dataCenterLatency)
  }
}

object EndpointActor {

  def props(system: System, endpointId: Int) = Props(classOf[EndpointActor], system, endpointId)

}
