package logic

import logic.Domain.{Id, Situation}
import logic.Solutions.{CacheServerAllocation, Solution}
import logic.Solver.{Context, ScoredSolution, iterate, populate, shuffle, takeDistant}

import scala.util.Random

class Solver(
  situation: Situation
) {

  var populationN = (Math.sqrt(situation.cacheServersN) / 2).toInt + 4
  var copCoef = 0.35
  var freshBloodRate = 0.25
  var freshBloodFrequency = 5
  var invasionFrequency = 20
  var invasionSurvivedRate = 0.10
  var bestSolution = ScoredSolution(Array(), 0)

  private var lastTimeScoreChanged = 0
  private var lastTimeFreshBlood = 0

  implicit val context = Context(situation)

  var population = populate(populationN)
  bestSolution = population.maxBy(_.score)

  /**
    *
    * @return true, if best solution got updated
    */
  def iterate(): Boolean = {

    population = Solver.iterate(population, copCoef)
    lastTimeScoreChanged += 1
    var res = false
    val newBest = population.maxBy(_.score)
    if(newBest.score > bestSolution.score) {
      bestSolution = newBest
      println(s"${newBest.score}, boost took iterations: $lastTimeScoreChanged")
      println()
      lastTimeScoreChanged = 0
      res = true
    }
    if (lastTimeFreshBlood == freshBloodFrequency) {
      //println("fresh blood!")
      val freshBloodN = (population.length * freshBloodRate).toInt
      population = (shuffle(population).take(population.length - freshBloodN) ++ populate(freshBloodN)).toArray
      lastTimeFreshBlood = 0
    }
    lastTimeFreshBlood += 1
    res
  }

  def changePopulationCount(count: Int): Unit = {

    if(count > populationN) {
      population = population ++ populate(count - populationN)
    } else {
      population = shuffle(population).take(count).toArray
    }

    populationN = count
    println(s"Population count changed: $count")
  }

  def invade(): Unit = {
    println("invasion!")
    val survived = (invasionSurvivedRate * populationN).ceil.toInt
    population =
      ((bestSolution +:
        takeDistant(bestSolution, population, survived - 1)) ++ populate(populationN - survived).toSeq).toArray
  }

}


object Solver {

  case class Context(situation: Situation) {

    def getVideoSize(id: Id): Int = situation.videos(id)

  }

  case class CacheServerInContext(videos: Set[Id], totalSize: Int){

    def canPut(videoId: Id)(implicit context: Context): Boolean =
      totalSize + context.getVideoSize(videoId) <= context.situation.cacheServerCapacity

    /**
      * Unsafe: does not check servers capacity
      *
      * @param videoId
      * @return
      */
    def put(videoId: Id)(implicit context: Context): CacheServerInContext =
      CacheServerInContext(videos + videoId, totalSize + context.getVideoSize(videoId))

  }

  case class ScoredSolution(allocations: Array[CacheServerInContext], score: Long)

  implicit def cacheServerInContextToCacheServerAllocation(cacheServerInContext: CacheServerInContext): CacheServerAllocation =
    CacheServerAllocation(cacheServerInContext.videos)
  implicit def scoredSolution2Solution(solution: ScoredSolution): Solution =
    Solution(solution.allocations.map(cacheServerInContextToCacheServerAllocation))
  def measureAllocation(cacheServerAllocation: CacheServerAllocation)(implicit context: Context): CacheServerInContext = {
    import cacheServerAllocation.videos
    import context._
    val totalSize = videos.par.aggregate(0)((acc, video) => acc + getVideoSize(video), _ + _)
    assert(totalSize <= situation.cacheServerCapacity)
    CacheServerInContext(videos, totalSize)
  }

  def populate(count: Int)(implicit context: Context): Array[ScoredSolution] = {

    import context._

    val random = new Random()
    val nextRandomVideoId = () => random.nextInt(situation.videos.length)
    val populateCacheServer = () => {
      def helper(allocation: CacheServerInContext): CacheServerInContext = {
        val candidateToAdd = nextRandomVideoId()
        if(allocation canPut candidateToAdd) {
          helper(allocation.put(candidateToAdd))
        }
        else {
          allocation
        }
      }
      helper(CacheServerInContext(Set.empty, 0))
    }


    (1 to count).toArray.par
      .map { _ =>

        val allocations =
          (1 to situation.cacheServersN)
            .map(_ => populateCacheServer()).toArray

        val score =
          ScoringFunction.score(situation, Solution(allocations.map(cacheServerInContextToCacheServerAllocation)))
        ScoredSolution(allocations, score)
      }.toArray
  }

  def iterate(
    solutions: Array[ScoredSolution],
    copCoef: Double
  )(implicit context: Context): Array[ScoredSolution] = {

    import context._

    val random = new Random()

    def butNot(k: Int, n: Int, not: Set[Int]): Int = {
      if (not contains k) {
        butNot((k + 1) % n, n, not)
      } else {
        k
      }
    }

    solutions.indices.par.map(index => {
      val solution = solutions(index)
      val iA = butNot(random.nextInt(solutions.length), solutions.length, Set(index))
      val iB = butNot(random.nextInt(solutions.length), solutions.length, Set(index, iA))
      val iC = butNot(random.nextInt(solutions.length), solutions.length, Set(index, iA, iB))
      val a = solutions(iA)
      val b = solutions(iB)
      val c = solutions(iC)
      val d = copulate(a, b, c, copCoef)
      val e = crossover(solution, d)
      val eScore = ScoringFunction.score(situation, e)
      if (eScore > solution.score) {
        ScoredSolution(e.cacheServerAllocations.map(measureAllocation), eScore)
      } else {
        solution
      }
    }).toArray
  }

  def copulate(
    a: Solution,
    b: Solution,
    c: Solution,
    copCoef: Double
  )(implicit context: Context): Solution = {
    diff(a, useCoef(diff(b, c), copCoef))
  }

  /*def crossover(a: Solution, b: Solution)(implicit context: Context): Solution = {
    def crossoverCacheServerSolution(
      alloc1: CacheServerAllocation,
      alloc2: CacheServerAllocation
    ): CacheServerAllocation = {
      val crv = shuffle(alloc1.videos.union(alloc2.videos).toSeq)
      CacheServerAllocation(takeUnderCapacity(crv, context.situation.cacheServerCapacity)._1.toSet)
    }
    Solution(
      (a.cacheServerAllocations zip b.cacheServerAllocations)
        .map{ case (alloc1, alloc2) => crossoverCacheServerSolution(alloc1, alloc2) }
    )
  }*/

  def crossover(a: Solution, b: Solution)(implicit context: Context): Solution = {
    val random = new Random()
    def crossoverCacheServerSolution(
      alloc1: CacheServerAllocation,
      alloc2: CacheServerAllocation
    ): CacheServerAllocation = {
      if(random.nextDouble() > 0.5) {
        alloc1
      } else{
        alloc2
      }
    }
    Solution(
      (a.cacheServerAllocations zip b.cacheServerAllocations)
        .map{ case (alloc1, alloc2) => crossoverCacheServerSolution(alloc1, alloc2) }
    )
  }

  def diff(a: Solution, b: Solution)(implicit  context: Context): Solution = {
    def diffAllocations(
      alloc1: CacheServerAllocation,
      alloc2: CacheServerAllocation
    ): CacheServerAllocation = {
      val left = alloc1.videos.diff(alloc2.videos)
      val right = alloc2.videos.diff(alloc1.videos)
      val leftAndRight = (left union right)
      val (res, total) = takeUnderCapacity(
        shuffle(leftAndRight.toSeq) ++ alloc1.videos.diff(leftAndRight).toSeq,
        context.situation.cacheServerCapacity
      )
      CacheServerInContext(res.toSet, total)
    }
    Solution(
      (a.cacheServerAllocations zip b.cacheServerAllocations)
        .map{ case (alloc1, alloc2) => diffAllocations(alloc1, alloc2) }
    )
  }

  def useCoef(value: Solution, coef: Double)(implicit context: Context): Solution = {
    import context._
    def useCoefOnCacheServer(cacheServerAllocation: CacheServerAllocation): CacheServerAllocation = {
      val (res, total) =
        takeUnderCapacity(
          shuffle(cacheServerAllocation.videos.toSeq), (coef * situation.cacheServerCapacity).toInt
        )
      CacheServerInContext(res.toSet, total)
    }

    Solution(value.cacheServerAllocations.map(useCoefOnCacheServer))
  }

  def takeUnderCapacity(videos: Seq[Id], capacity: Int)(implicit context: Context): (Seq[Id], Int) = {
    import context._
    videos.foldLeft(Seq.empty[Id], 0){
      case ((acc, accSize), id)  => {
        val size = getVideoSize(id)
        if(accSize + size <= capacity) {
          (id +: acc, accSize + size)
        }
        else {
          (acc, accSize)
        }
      }
    }
  }

  def shuffle[A](seq: Seq[A]): Stream[A] = {
    val random = new Random()
    def randomElementAndRest(s: Seq[A]): (A, Seq[A]) = {
      val i = random.nextInt(s.length)
      val (first, value +: rest) = s.splitAt(i)
      (value, first ++ rest)
    }
    def helper(s: Seq[A]): Stream[A] = s match {
      case Seq() => Stream.empty
      case _ =>
        val (value, rest) = randomElementAndRest(s)
        value #:: helper(rest)
    }
    helper(seq)
  }

  def distance(a: ScoredSolution, b: ScoredSolution)(implicit context: Context): Int = {
    import context._
    (a.cacheServerAllocations zip b.cacheServerAllocations).par.map{
      case (ca, cb) =>
        (ca.videos.diff(cb.videos) ++ cb.videos.diff(ca.videos)).foldLeft(0)((acc, video) => acc + getVideoSize(video))
    }.sum
  }

  def takeDistant(s: ScoredSolution, ss: Seq[ScoredSolution], n: Int)(implicit context: Context): Seq[ScoredSolution] = {

    def measure(s: ScoredSolution, ss: Seq[(ScoredSolution, Int)]): Seq[(ScoredSolution, Int)] = {

      ss.par.map{ case (solution, distanceAcc) => (solution, distance(s, solution) + distanceAcc)}.toList.sortBy(_._2)

    }

    def helper(s: ScoredSolution, ss: Seq[(ScoredSolution, Int)]): Stream[ScoredSolution] = {

      val (next::rest) = measure(s, ss)
      next._1 #:: helper(next._1, rest)

    }

    helper(s, ss.map((_ , 0))).take(n).toList

  }

}
