val Versions =
  new {
    val tapir = "0.20.2"
    val http4s = "0.23.11"
    val logback = "1.2.11"
  }

ThisBuild / scalaVersion := "3.1.2"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

Global / onChangedBuildSource := ReloadOnSourceChanges

//
val commonSettings: Seq[Setting[_]] = Seq(
  scalacOptions -= "-Xfatal-warnings",
  scalacOptions += "-source:future",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.3.11",
    "co.fs2" %% "fs2-core" % "3.2.7",
    // "org.typelevel" %% "cats-mtl" % "1.2.1",
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
    "com.disneystreaming" %% "weaver-cats" % "0.7.11" % Test,
    "com.disneystreaming" %% "weaver-scalacheck" % "0.7.11" % Test,
    compilerPlugin("org.polyvariant" % "better-tostring" % "0.3.15" cross CrossVersion.full),
  ),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
)

val nativeImageSettings: Seq[Setting[_]] = Seq(
  Compile / mainClass := Some("steve.Main"),
  nativeImageVersion := "21.2.0",
  nativeImageOptions ++= Seq(
    s"-H:ReflectionConfigurationFiles=${(Compile / resourceDirectory).value / "reflect-config.json"}",
    s"-H:ResourceConfigurationFiles=${(Compile / resourceDirectory).value / "resource-config.json"}",
    "-H:+ReportExceptionStackTraces",
    "--no-fallback",
    "--allow-incomplete-classpath",
  ),
  nativeImageAgentMerge := true,
  nativeImageReady := { () => () },
)

val shared = project.settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "fs2" % "3.5.2",
    "io.circe" %% "circe-fs2" % "0.14.0",
    "org.typelevel" %% "log4cats-core" % "2.3.1",
    "org.typelevel" %% "log4cats-noop" % "2.3.1",
    "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir,
  ),
)

def full(p: Project) = p % "test->test;compile->compile"

val server = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % Versions.http4s,
      "org.http4s" %% "http4s-ember-server" % Versions.http4s,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Versions.tapir,
      "ch.qos.logback" % "logback-classic" % Versions.logback,
      "dev.optics" %% "monocle-core" % "3.1.0",
      "org.http4s" %% "http4s-circe" % Versions.http4s % Test,
      "org.http4s" %% "http4s-client" % Versions.http4s % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
    ),
  )
  .dependsOn(full(shared))

val client = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client" % Versions.http4s,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-client" % Versions.tapir,
      "ch.qos.logback" % "logback-classic" % Versions.logback,
      "com.monovore" %% "decline-effect" % "2.2.0",
    ),
    nativeImageSettings,
  )
  .enablePlugins(NativeImagePlugin)
  // todo: remove dependency on server
  .dependsOn(full(shared), server)

val e2e = project
  .settings(commonSettings)
  .dependsOn(full(server), full(client))

val root = project
  .in(file("."))
  .settings(publish := {}, publish / skip := true)
  .aggregate(server, client, shared, e2e)
