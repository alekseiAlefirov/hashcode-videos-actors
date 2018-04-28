package logic

import logic.Domain.{Id, Latency, Situation}
import logic.Solutions.Solution

object ScoringFunction{

  def score(situation: Situation, solution: Solution): Long = {
    import situation._


    val totalGain = situation.requests.par
      .map{ request =>
        import request._
        val endpoint = endpoints(endpointId)
        val dataCenterLatency = endpoint.dataCenterLatency

        def checkCacheServer(currentLatency: Latency, connection: (Id, Latency)): Latency = {
          val (cacheServer, latency) = connection
          if (latency < currentLatency
            && solution.cacheServerAllocations(cacheServer).videos.contains(videoId)) {
            latency
          }
          else
            currentLatency
        }

        val optimalLatency  =
          endpoint.connections.par.aggregate(dataCenterLatency) ({
            case (opt, connection) =>
              checkCacheServer(opt, connection)
          },
            _ min _
          )

        (dataCenterLatency - optimalLatency) * times * 1000L
      }.sum

    totalGain / requestsN
  }

}