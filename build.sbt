
name := """dslink-scala-cogswell-pubsub"""

organization := "io.cogswell"

version := "1.0.0"

scalaVersion := "2.11.7"

resolvers += Resolver.jcenterRepo

enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  "org.iot-dsa" % "sdk-dslink-scala_2.11" % "0.4.0",
  "io.cogswell" % "cogs-java-client-sdk" % "2.0.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
