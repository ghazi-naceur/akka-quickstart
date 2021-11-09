name := "akka-quickstart"

version := "0.1"

scalaVersion := "2.12.7"

lazy val akkaVersion = "2.6.12"
val akkaHttpVersion = "10.1.12"
val commonsIOVersion = "2.6"
val sparkVersion = "3.0.0"
val twitter4jVersion = "4.0.7"
val log4jVersion = "2.4.1"
val awsVersion = "1.9.0"
val kafkaVersion = "2.4.0"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "commons-io" % "commons-io" % commonsIOVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.twitter4j" % "twitter4j-core" % twitter4jVersion,
  "org.twitter4j" % "twitter4j-stream" % twitter4jVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsVersion,
  "org.apache.kafka" %% "kafka" % kafkaVersion,
  "org.apache.kafka" % "kafka-streams" % kafkaVersion
)
