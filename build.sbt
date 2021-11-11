val Versions =
  new {
    val tapir = "0.19.0-M11"
    val http4s = "0.23.5"
    val logback = "1.2.7"
  }

ThisBuild / scalaVersion := "3.1.0-RC2"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

val commonSettings: Seq[Setting[_]] = Seq(
  scalacOptions -= "-Xfatal-warnings",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.2.9",
    // "org.typelevel" %% "cats-mtl" % "1.2.1",
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test,
    compilerPlugin("org.polyvariant" % "better-tostring" % "0.3.9" cross CrossVersion.full),
  ),
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
      "org.http4s" %% "http4s-circe" % Versions.http4s % Test,
      "org.http4s" %% "http4s-client" % Versions.http4s % Test,
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
    ),
    nativeImageSettings,
  )
  .enablePlugins(NativeImagePlugin)
  .dependsOn(full(shared))

val e2e = project
  .settings(commonSettings)
  .dependsOn(full(server), full(client))

val root = project
  .in(file("."))
  .settings(publish := {}, publish / skip := true)
  .aggregate(server, client, shared, e2e)
