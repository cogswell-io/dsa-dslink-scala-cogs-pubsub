name := """dslink-scala-cogswell-pubsub"""

version := "1.0"

scalaVersion := "2.11.7"

resolvers += Resolver.jcenterRepo

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// https://mvnrepository.com/artifact/org.iot-dsa/sdk-dslink-scala_2.11
libraryDependencies += "org.iot-dsa" % "sdk-dslink-scala_2.11" % "0.4.0"

// Cogswell client SDK
libraryDependencies += "io.cogswell" % "cogs-java-client-sdk" % "2.0.0"

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

