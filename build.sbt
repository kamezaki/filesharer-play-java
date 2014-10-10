name := """filesharer-play-java"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3",
  "com.amazonaws" % "aws-java-sdk" % "1.8.9.1",
  "commons-io" % "commons-io" % "2.4"
)
