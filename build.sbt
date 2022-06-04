ThisBuild / scalaVersion := "2.13.7"
ThisBuild / organization := "com.edmisml"
ThisBuild / organizationName := "edmisml"
ThisBuild / version := "0.1.0-SNAPSHOT"

val Versions = new {
  val zio        = "2.0.0-RC6"
  val zioLogging = "2.0.0-RC10"
  val zioInterop = "3.3.0-RC7"
  val http4s     = "0.23.12"
  val doobie     = "1.0.0-RC2"
  val ciris      = "2.3.2"
  val circe      = "0.14.2"
  val logback    = "1.2.4"
}

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
lazy val todo = (project in file("modules/todo"))
  .enablePlugins(DockerPlugin)
  .settings(
    name := "zio-todo-app",
    Docker / packageName := "edmisml/zio-todo-app",
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    dockerUpdateLatest := true,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scalacOptions -= "-Xfatal-warnings",
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Seq(
      "dev.zio"        %% "zio"                 % Versions.zio,
      "dev.zio"        %% "zio-logging"         % Versions.zioLogging,
      "dev.zio"        %% "zio-logging-slf4j"   % Versions.zioLogging,
      "dev.zio"        %% "zio-macros"          % Versions.zio,
      "dev.zio"        %% "zio-interop-cats"    % Versions.zioInterop,
      "org.http4s"     %% "http4s-circe"        % Versions.http4s,
      "ch.qos.logback" % "logback-classic"      % Versions.logback % Runtime,
      "io.circe"       %% "circe-generic"       % Versions.circe,
      "io.circe"       %% "circe-refined"       % Versions.circe,
      "org.http4s"     %% "http4s-dsl"          % Versions.http4s,
      "is.cir"         %% "ciris"               % Versions.ciris,
      "org.http4s"     %% "http4s-ember-server" % Versions.http4s,
      "org.tpolecat"   %% "doobie-core"         % Versions.doobie,
      "org.tpolecat"   %% "doobie-h2"           % Versions.doobie,
      "org.tpolecat"   %% "doobie-refined"      % Versions.doobie,
      "dev.zio"        %% "zio-test"            % Versions.zio % Test,
      "dev.zio"        %% "zio-test-sbt"        % Versions.zio % Test
    )
  )

lazy val root = (project in file("."))
  .settings(name := "todo-root", publish := {}, publish / skip := true)
  .aggregate(todo)
