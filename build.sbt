organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8" % Test

lazy val `aloha` = (project in file("."))
  .aggregate(`aloha-api`, `aloha-impl`,`aloha-stream-api`,`aloha-stream-impl`)

lazy val `aloha-api` = (project in file("aloha-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
)
  )

lazy val `aloha-impl` = (project in file("aloha-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`aloha-api`,`aloha-stream-api`)

lazy val `aloha-stream-api` = (project in file("aloha-stream-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `aloha-stream-impl` = (project in file("aloha-stream-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`aloha-stream-api`)
