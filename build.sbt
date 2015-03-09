name := """building-microservice"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  filters,
  "org.jsoup" % "jsoup" % "1.8.1",
  "org.influxdb" % "influxdb-java" % "1.5"
)
