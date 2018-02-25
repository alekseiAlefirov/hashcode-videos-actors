scalaVersion := "2.12.4"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"


libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalafx" %% "scalafx" % "8.0.144-R12",
  "com.typesafe.akka" %% "akka-actor" % "2.5.10"
)