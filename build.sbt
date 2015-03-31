import NativePackagerKeys._

name := """building-microservice"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  filters,
  "org.jsoup" % "jsoup" % "1.8.1"
)

// Docker Config
maintainer := "roymeissn@gmail.com"

dockerExposedPorts in Docker := Seq(9000)
