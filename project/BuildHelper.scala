import sbt._
import sbt.Keys._

object Versions {
  val zio      = "2.1.17"
  val munit    = "1.2.4"
  val scala3   = "3.3.6"
  val scala213 = "2.13.16"
}

object BuildHelper {

  private val stdOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked"
  )

  private val stdOpts3 = Seq(
    "-Xfatal-warnings",
    "-source:future"
  )

  private val stdOpts213 = Seq(
    "-Xfatal-warnings",
    "-Xlint:_,-type-parameter-shadow",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )

  def stdSettings(prjName: String): Seq[Setting[_]] =
    Seq(
      name              := prjName,
      crossScalaVersions := Seq(Versions.scala3, Versions.scala213),
      scalaVersion      := Versions.scala3,
      scalacOptions     := stdOptions ++ (
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((3, _))    => stdOpts3
          case Some((2, 13))   => stdOpts213
          case _               => Seq.empty
        }
      ),
      Test / parallelExecution := false,
      incOptions ~= (_.withLogRecompileOnMacro(false)),
      autoAPIMappings := true
    )

  /** Maven Central metadata (version is set by sbt-dynver via sbt-ci-release). */
  def publishSettings: Seq[Setting[_]] =
    Seq(
      versionScheme      := Some("semver-spec"),
      pomIncludeRepository := { _ => false },
      publishMavenStyle  := true,
      Compile / doc / sources := Seq.empty,
      Compile / packageDoc / publishArtifact := false
    )
}
