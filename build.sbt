import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import com.scalapenos.sbt.prompt._
import Dependencies._
import microsites.ExtraMdFileConfig

ThisBuild / name := """fs2-rabbit"""
ThisBuild / crossScalaVersions := List("2.12.10", "2.13.1")
ThisBuild / organization := "dev.profunktor"
ThisBuild / homepage := Some(url("https://fs2-rabbit.profunktor.dev/"))
ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer(
    "gvolpe",
    "Gabriel Volpe",
    "volpegabriel@gmail.com",
    url("https://gvolpe.github.io")
  )
)

promptTheme := PromptTheme(
  List(
    text("[sbt] ", fg(105)),
    text(_ => "fs2-rabbit", fg(15)).padRight(" λ ")
  )
)

def maxClassFileName(v: String) =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => List.empty[String]
    case _             => List("-Xmax-classfile-name", "100")
  }

val commonSettings = List(
  organizationName := "ProfunKtor",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://fs2-rabbit.profunktor.dev/")),
  headerLicense := Some(HeaderLicense.ALv2("2017-2020", "ProfunKtor")),
  scalacOptions in (Compile, doc) ++= List("-no-link-warnings"),
  scalacOptions ++= maxClassFileName(scalaVersion.value),
  libraryDependencies ++= {
    List(
      compilerPlugin(Libraries.kindProjector),
      compilerPlugin(Libraries.betterMonadicFor),
      Libraries.amqpClient,
      Libraries.catsEffect,
      Libraries.fs2Core,
      Libraries.scalaTest               % Test,
      Libraries.scalaCheck              % Test,
      Libraries.scalaTestPlusScalaCheck % Test
    )
  },
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalafmtOnCompile := true
)

def CoreDependencies(scalaVersionStr: String): List[ModuleID] =
  List(
    Libraries.scodecCats,
    Libraries.logback % Test
  )

def JsonDependencies(scalaVersionStr: String): List[ModuleID] =
  List(
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeParser
  )

def ExamplesDependencies(scalaVersionStr: String): List[ModuleID] =
  List(
    Libraries.logback % "runtime",
    Libraries.monix,
    Libraries.zioCore,
    Libraries.zioCats,
    Libraries.dropwizard,
    Libraries.dropwizardJmx
  )

def TestKitDependencies(scalaVersionStr: String): List[ModuleID] = List(Libraries.scalaCheck)

def TestsDependencies(scalaVersionStr: String): List[ModuleID] =
  List(
    Libraries.disciplineScalaCheck % Test,
    Libraries.catsLaws             % Test,
    Libraries.catsKernelLaws       % Test
  )

lazy val noPublish = List(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip in publish := true
)

lazy val `fs2-rabbit-root` = project
  .in(file("."))
  .aggregate(`fs2-rabbit`, `fs2-rabbit-circe`, tests, examples, microsite, `fs2-rabbit-testkit`)
  .settings(noPublish)

lazy val `fs2-rabbit` = project
  .in(file("core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= CoreDependencies(scalaVersion.value))
  .settings(parallelExecution in Test := false)
  .enablePlugins(AutomateHeaderPlugin)

lazy val `fs2-rabbit-circe` = project
  .in(file("json-circe"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= JsonDependencies(scalaVersion.value))
  .settings(parallelExecution in Test := false)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`fs2-rabbit`)

lazy val tests = project
  .in(file("tests"))
  .settings(commonSettings: _*)
  .settings(noPublish)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(libraryDependencies ++= TestsDependencies(scalaVersion.value))
  .settings(parallelExecution in Test := false)
  .dependsOn(`fs2-rabbit`, `fs2-rabbit-testkit`)

lazy val examples = project
  .in(file("examples"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= ExamplesDependencies(scalaVersion.value))
  .settings(noPublish)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`fs2-rabbit`, `fs2-rabbit-circe`)

lazy val `fs2-rabbit-testkit` = project
  .in(file("testkit"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= TestKitDependencies(scalaVersion.value))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`fs2-rabbit`)

lazy val microsite = project
  .in(file("site"))
  .enablePlugins(MicrositesPlugin)
  .settings(commonSettings: _*)
  .settings(noPublish)
  .settings(
    micrositeName := "Fs2 Rabbit",
    micrositeDescription := "RabbitMQ stream-based client",
    micrositeAuthor := "ProfunKtor",
    micrositeGithubOwner := "profunktor",
    micrositeGithubRepo := "fs2-rabbit",
    micrositeBaseUrl := "",
    micrositeExtraMdFiles := Map(
      file("README.md")          -> ExtraMdFileConfig(
        "index.md",
        "home",
        Map("title" -> "Home", "position" -> "0")
      ),
      file("CODE_OF_CONDUCT.md") -> ExtraMdFileConfig(
        "CODE_OF_CONDUCT.md",
        "page",
        Map("title" -> "Code of Conduct")
      )
    ),
    micrositeExtraMdFilesOutput := (resourceManaged in Compile).value / "jekyll",
    micrositeGitterChannel := true,
    micrositeGitterChannelUrl := "profunktor-dev/fs2-rabbit",
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    scalacOptions --= List(
      "-Werror",
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Xlint:-missing-interpolator,_"
    )
  )
  .dependsOn(`fs2-rabbit`, `fs2-rabbit-circe`, `examples`)

// CI build
addCommandAlias("buildFs2Rabbit", ";clean;+test;mdoc")
