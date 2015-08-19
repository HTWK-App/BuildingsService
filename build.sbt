name := """building-microservice"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  filters,
  ws,
  "org.jsoup" % "jsoup" % "1.8.3",
  "org.apache.jena" % "jena-core" % "2.13.0"
)
