package logic

import logic.Domain.Id

object Solutions {

  case class CacheServerAllocation(videos: Set[Id])

  case class Solution(cacheServerAllocations: Array[CacheServerAllocation])

}