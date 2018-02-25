import logic.Domain._
import logic.Solutions.{CacheServerAllocation, Solution}
import org.scalatest._

class ScoringFunctionTest extends FlatSpec with Matchers {

  //behavior of ScoringFunction

  import logic.ScoringFunction._

  it should "score for one cache server" in {
    //GIVEN
    val situation = Situation(
      //DataCenter(Map(0 -> 500)),
      videos = Array(100),
      endpoints = Array(Endpoint(500, Map(0 -> 200))),
      cacheServersN = 1,
      cacheServerCapacity = 1000,
      requests = List(Request(0, 0, 3))
    )

    val solution =
      Solution(
        Array(
          CacheServerAllocation(Set(0))
        )
      )
    // WHEN
    val expectedResult = 300000
    //THEN
    score(situation, solution) shouldBe expectedResult
  }

  it should "score for multiple cache servers and videos" in {
    //GIVEN
    val situation = Situation(
      videos = Array(50, 50, 80, 30, 110),
      endpoints = Array(
        Endpoint(1000, Map(0 -> 100, 1 -> 300, 2 -> 200)),
        Endpoint(500, Map.empty)),
      cacheServersN = 3,
      cacheServerCapacity = 100,
      requests = List(
        Request(3, 0, 1500),
        Request(0, 1, 1000),
        Request(4, 0, 500),
        Request(1, 0, 1000)
      )
    )
    val solution = Solution(
      Array(
        CacheServerAllocation(Set(2)),
        CacheServerAllocation(Set(3, 1)),
        CacheServerAllocation(Set(0 ,1))
      )
    )
    // WHEN
    val expectedResult = 462500
    //THEN
    score(situation, solution) shouldBe expectedResult
  }

  it should "score for multiple cache servers and videos 2" in {
    //GIVEN
    val situation = Situation(
      videos = Array(50, 50, 80, 30, 110),
      endpoints = Array(
        Endpoint(1000, Map(0 -> 100, 1 -> 300, 2 -> 200)),
        Endpoint(500, Map.empty)),
      cacheServersN = 3,
      cacheServerCapacity = 100,
      requests = List(
        Request(3, 0, 1500),
        Request(0, 1, 1000),
        Request(4, 0, 500),
        Request(1, 0, 1000)
      )
    )

    //3
    //0 1 3
    //1 0
    //2
    val solution = Solution(
      Array(
        CacheServerAllocation(Set(2)),
        CacheServerAllocation(Set(0)),
        CacheServerAllocation(Set(3))
      )
    )
    // WHEN
    val expectedResult = 300000
    //THEN
    score(situation, solution) shouldBe expectedResult
  }

  it should "score for multiple cache servers and videos 3" in {
    //GIVEN
    val situation = Situation(
      videos = Array(50, 50, 80, 30, 110),
      endpoints = Array(
        Endpoint(1000, Map(0 -> 100, 1 -> 300, 2 -> 200)),
        Endpoint(500, Map.empty)),
      cacheServersN = 3,
      cacheServerCapacity = 100,
      requests = List(
        Request(3, 0, 1500),
        Request(0, 1, 1000),
        Request(4, 0, 500),
        Request(1, 0, 1000)
      )
    )

    val solution = Solution(
      Array(
        CacheServerAllocation(Set(0, 3)),
        CacheServerAllocation(Set(1, 0)),
        CacheServerAllocation(Set())
      )
    )
    // WHEN
    val expectedResult = 512500
    //THEN
    score(situation, solution) shouldBe expectedResult
  }

}
