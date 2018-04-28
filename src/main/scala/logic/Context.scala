package logic

import logic.Domain.{Id, Situation}

case class Context(situation: Situation) {

  def getVideoSize(id: Id): Int = situation.videos(id)

  def weight(videos: Iterable[Id]) = videos.map(getVideoSize).sum

  //val requestsByEndPoint

}
