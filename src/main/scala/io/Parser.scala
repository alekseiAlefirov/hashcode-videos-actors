package io

import logic.Domain.{Endpoint, Request, Situation, Size}

import scala.io.Source

object Parser {

  def parseFile(filePath: String): Situation = {
    val linesIter = Source.fromFile(filePath).getLines()
    parse(linesIter)
  }

  def parse(linesIter: Iterator[String]) : Situation = {
    val (
      videosN,
      endpointsN,
      requestsN,
      cacheServersN,
      capacity
      ) = parseCounts(linesIter.next())
    val videos = parseVideos(linesIter.next(), videosN)
    val endpoints =
      (0 until endpointsN).map( _ => parseEndpoint(linesIter)).toArray
    val (requests) =
      (0 until requestsN).map( _ => parseRequest(linesIter))
    Situation(
      videos,
      endpoints,
      cacheServersN,
      capacity,
      requests
    )
  }

  def parseCounts(line: String): (Int, Int, Int, Int, Int) = {
    val regex = """(\d+) (\d+) (\d+) (\d+) (\d+)""".r
    line match { case regex(v, e, r, c, x) => (v.toInt, e.toInt, r.toInt, c.toInt, x.toInt) }
  }

  def parseVideos(line: String, n: Int): Array[Size] = {
    val sizeStrs = line.split(" ")
    sizeStrs.map(_.toInt)
  }

  def parseEndpoint(lines: Iterator[String]): Endpoint = {
    val descRegex = """(\d+) (\d+)""".r
    val (latency, cacheServersN) = lines.next() match { case descRegex(l, cN) => (l.toInt, cN.toInt ) }
    val connections =
      (0 until cacheServersN).map { _ =>
        val regex = """(\d+) (\d+)""".r
        lines.next() match { case regex(cId, cL) => (cId.toInt, cL.toInt) }
      }.toMap
    Endpoint(latency, connections)
  }

  def parseRequest(lines: Iterator[String]): Request = {
    val regex = """(\d+) (\d+) (\d+)""".r
    lines.next() match {
      case regex(videoId, endpointId, n) => Request(videoId.toInt, endpointId.toInt, n.toInt)
    }
  }

}
