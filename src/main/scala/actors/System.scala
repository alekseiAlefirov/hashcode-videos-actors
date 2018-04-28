package actors

import akka.actor.ActorRef
import logic.Domain.Id
import logic.{Context, RandomHelpers}
import logic.Solutions.CacheServerAllocation

import scala.collection.mutable
import scala.util.Random

class System(val context: Context) {

  def situation = context.situation

  private val _requestsByEndpoint =
    situation.requests.groupBy(_.endpointId)
      //merge
      .mapValues(_.groupBy(_.videoId).mapValues(_.reduce(_ + _)).values.toList)
  def requestsByEndpoint(endpointId: Id) = _requestsByEndpoint(endpointId)

  private val _requestsByVideo =
    situation.requests.groupBy(_.videoId)
      //merge
      .mapValues(_.groupBy(_.endpointId).mapValues(_.reduce(_ + _)).values.toList)
  def requestsByVideo(videoId: Id) = _requestsByVideo(videoId)

  /*def interesandsByVideoAndCacheServer(video: Id, cacheServerId: Id): Set[Id] = {
    requestsByVideo(video).map(_.endpointId).map(endpointId => (endpointId, situation.endpoints(endpointId)))

  }*/

  /*private val _endpointsByCacheServer =
    situation.endpoints.indices.foldLeft(
      (0 until situation.cacheServersN).map((_, Set.empty[Id])).toMap
    ) {
      case (acc, endpointId) =>
        val endpoint = situation.endpoints(endpointId)
        endpoint.connections.keys.foldLeft(acc)(
          (acc2, cacheServerId) => acc2.updated(cacheServerId, acc2(cacheServerId) + endpointId)
        )
    }
  def endpointsByCacheServer(cacheServerId: Id) = _endpointsByCacheServer(cacheServerId)*/

  /*private val cacheServerAllocations: Array[(CacheServerAllocation, Int)] = {
    RandomHelpers.randomSolution(new Random(), context).cacheServerAllocations.map{ allocation =>
      totalSize = allocation.videos.map(context.getVideoSize).sum
      (allocation, totalSize)
    }
  }

  private val allocationsByVideo = mutable.Map.empty[Id, Set[Id]]
  for {
    cacheServerId <- cacheServerAllocations.indices
    videoId <- cacheServerAllocations(cacheServerId)
  } allocationsByVideo(videoId) = allocationsByVideo(videoId) + cacheServerId

  def addVideo(cacheServer: Id, video: Id) = {
    val (oldAl, oldSize) = cacheServerAllocations(cacheServer)
    val newAl = CacheServerAllocation(oldAl.videos + video)
    val newSize = oldSize + context.getVideoSize(video)
    cacheServerAllocations(cacheServer) = (newAl, newSize)
    allocationsByVideo(video) = allocationsByVideo(video) + cacheServer
  }

  def deleteVideo(cacheServer: Id, video: Id) = {
    val (oldAl, oldSize) = cacheServerAllocations(cacheServer)
    val newAl = CacheServerAllocation(oldAl.videos - video)
    val newSize = oldSize - context.getVideoSize(video)
    cacheServerAllocations(cacheServer) = (newAl, newSize)
    allocationsByVideo(video) = allocationsByVideo(video) - cacheServer
  }*/

  //def getCacheServerAllocation(cacheServer: Id): CacheServerAllocation = cacheServerAllocations(cacheServer)
  //def getVideoAllocation(video: Id): Set[Id] = allocationsByVideo(video)

  var systemActor = ActorRef.noSender
  var endpointActors = Array.empty[ActorRef]
  var cacheServerActors = Array.empty[ActorRef]

}
