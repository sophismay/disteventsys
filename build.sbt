name := "diseventsys"

version := "1.0"

scalaVersion := "2.11.0"

mainClass in (Compile, run) := Some("de.tud.disteventsys.DisEventSys")

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.12",
  "com.espertech" % "esper" % "4.11.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
)