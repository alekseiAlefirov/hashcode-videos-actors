package thirdparty

//This is based on code by
// ©2012 Viktor Klang
//Taken from here: https://gist.github.com/viktorklang/2422443
//
//also this advice by Rüdiger Klaehn was of help
// https://stackoverflow.com/questions/20828726/javafx2-or-scalafx-akka/20836997#20836997

import akka.dispatch.{DispatcherPrerequisites, ExecutorServiceConfigurator, ExecutorServiceFactory}
import com.typesafe.config.Config
import java.util.concurrent.{AbstractExecutorService, ExecutorService, ThreadFactory, TimeUnit}
import java.util.Collections

import scalafx.application.Platform


object ScalaFxExecutorService extends AbstractExecutorService {
  def execute(command: Runnable) = Platform.runLater(command)
  def shutdown(): Unit = ()
  def shutdownNow() = Collections.emptyList[Runnable]
  def isShutdown = false
  def isTerminated = false
  def awaitTermination(l: Long, timeUnit: TimeUnit) = true
}

// Then we create an ExecutorServiceConfigurator so that Akka can use our SwingExecutorService for the dispatchers
class ScalaFxEventThreadExecutorServiceConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {
  private val f = new ExecutorServiceFactory { def createExecutorService: ExecutorService = ScalaFxExecutorService }
  def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = f
}
