ThisBuild / version := "0.1.0-SNAPSHOT"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.17",
  // Add other Akka modules as needed (e.g., akka-stream, akka-http)
)
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.17"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "pl3_project"
  )
