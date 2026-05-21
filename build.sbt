import BuildHelper._

inThisBuild(
  List(
    organization := "dev.zio",
    homepage     := Some(url("https://github.com/zio/zio-ulid")),
    description  := "Type-safe, purely functional ULID generation for ZIO applications.",
    licenses     := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/zio/zio-ulid"),
        "scm:git@github.com:zio/zio-ulid.git"
      )
    ),
    developers := List(
      Developer(
        "er-dhirendra",
        "Dhirendra Kumar Kashyap",
        "dhirendra@example.com",
        url("https://github.com/Er-dhirendra")
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "; scalafmtSbtCheck; scalafmtCheckAll")

lazy val root = project
  .in(file("."))
  .settings(
    name           := "zio-ulid",
    publish / skip := true
  )
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(stdSettings("zio-ulid"))
  .settings(publishSettings)
  .settings(
    moduleName := "zio-ulid",
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio"   % Versions.zio,
      "org.scalameta" %% "munit" % Versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
