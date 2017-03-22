
name := """dslink-scala-cogswell-pubsub"""

organization := "io.cogswell"

version := "1.0.0"

scalaVersion := "2.11.7"

resolvers += Resolver.jcenterRepo

enablePlugins(JavaAppPackaging)

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

mappings in Universal += file("dslink.json") -> "dslink.json"

libraryDependencies ++= Seq(
  "org.iot-dsa" % "dslink" % "0.16.0",
  "io.cogswell" % "cogs-java-client-sdk" % "2.0.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
