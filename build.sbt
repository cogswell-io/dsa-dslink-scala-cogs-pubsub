
name := """dslink-scala-cogswell-pubsub"""

organization := "io.cogswell"

version := "1.0.0"

scalaVersion := "2.11.8"

resolvers += Resolver.jcenterRepo

enablePlugins(JavaAppPackaging)

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

mappings in Universal += file("dslink.json") -> "dslink.json"

libraryDependencies ++= Seq(
  "org.scaldi" % "scaldi_2.11" % "0.5.8",
  "org.iot-dsa" % "dslink" % "0.16.0",
  "io.cogswell" % "cogs-java-client-sdk" % "2.0.0",
  "joda-time" % "joda-time" % "2.9.7",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
