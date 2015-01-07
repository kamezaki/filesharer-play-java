name := """filesharer-play-java"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.feth" %% "play-authenticate" % "0.6.8",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3",
  "com.amazonaws" % "aws-java-sdk" % "1.8.9.1",
  "commons-io" % "commons-io" % "2.4",
  javaJdbc,
  javaEbean,
  cache,
  javaWs
)
