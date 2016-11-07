name := """NLP4L-LTR_Support"""

organization := "org.nlp4l"

version := "0.2.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "com.typesafe.play" %% "play-slick" % "1.1.0",
  "mysql" % "mysql-connector-java" % "5.1.38",
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc41",
  "org.apache.lucene" % "lucene-analyzers-common" % "5.4.1",
  "org.apache.lucene" % "lucene-analyzers-kuromoji" % "5.4.1",
  "org.apache.lucene" % "lucene-suggest" % "5.4.1",
  "org.apache.lucene" % "lucene-backward-codecs" % "5.4.1",
  "org.apache.solr" % "solr-solrj" % "5.4.1",
  "org.apache.opennlp" % "opennlp-tools" % "1.6.0",
  "org.apache.spark" %% "spark-core" % "1.6.1" exclude("org.slf4j", "slf4j-log4j12"),
  "org.apache.spark" %% "spark-mllib" % "1.6.1",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "bootstrap" % "3.3.5",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.1.0",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3"
)

dependencyOverrides ++= Set(
  // Spark is expecting jackson-databind 2.4.4
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

unmanagedBase := baseDirectory.value / "lib"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
