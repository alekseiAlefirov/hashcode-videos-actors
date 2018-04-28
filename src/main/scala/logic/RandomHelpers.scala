package logic

import logic.Solutions.{CacheServerAllocation, Solution}

import scala.util.Random

object RandomHelpers {

  def randomSolution(implicit random: Random, context: Context): Solution = {
    import context._
    val nextRandomVideoId = () => random.nextInt(situation.videos.length)
    val populateCacheServer = () => {
      def helper(allocation: CacheServerAllocation, totalSize: Int): CacheServerAllocation = {
        val candidateToAdd = nextRandomVideoId()
        if(allocation.videos.contains(candidateToAdd)) {
          helper(allocation, totalSize)
        } else {
          if(totalSize + getVideoSize(candidateToAdd) < situation.cacheServerCapacity) {
            helper(
              CacheServerAllocation(allocation.videos + candidateToAdd),
              totalSize + getVideoSize(candidateToAdd)
            )
          }
          else {
            allocation
          }
        }
      }
      helper(CacheServerAllocation(Set.empty), 0)
    }

    Solution(
      (1 to situation.cacheServersN).map(_ => populateCacheServer()).toArray
    )
  }

  def takeRandomAndRest[A](seq: Seq[A])(implicit random: Random): (A, Seq[A]) = {
    val index = random.nextInt(seq.length)
    //println(s"takeRandom: $seq, $index")
    val (left, value+:right) = seq.splitAt(index)
    (value, left ++ right)
  }


}
