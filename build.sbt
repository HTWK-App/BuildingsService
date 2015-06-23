name := """building-microservice"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  filters,
  "org.jsoup" % "jsoup" % "1.8.2",
  "org.apache.jena" % "jena-core" % "2.13.0"
)
