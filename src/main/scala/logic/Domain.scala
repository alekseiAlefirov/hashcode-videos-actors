package logic


object Domain{

    type Id = Int
    type Latency = Int
    type Size = Int

    //case class DataCenter(connections: Map[Id, Latency])

    //case class CacheServer(id: Id, connections: Map[Id, Latency])

    case class Endpoint(dataCenterLatency: Latency, connections: Map[Id, Latency])

    case class Request(videoId: Id, endpointId: Id, times: Int)

    case class Situation(
      //dataCenter: DataCenter,
      videos: Array[Size],
      endpoints: Array[Endpoint],
      cacheServersN: Int,
      cacheServerCapacity: Int,
      requests: Seq[Request]
    ) {

      val requestsN = requests.map(_.times : Long).sum

    }

}
