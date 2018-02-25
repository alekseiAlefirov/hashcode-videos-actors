package io


import logic.Solutions._

object Printer {

  val solution = Solution(
    Array(
      CacheServerAllocation(Set(2)),
      CacheServerAllocation(Set(3, 1)),
      CacheServerAllocation(Set(0 ,1))
    )
  )

  def printSolution(solution: Solution): Unit = {
    import solution._
    println(cacheServerAllocations.length)
    cacheServerAllocations.indices.foreach(i => {
      println(i.toString + ' ' + cacheServerAllocations(i).videos.mkString(" "))
    })
  }

}
