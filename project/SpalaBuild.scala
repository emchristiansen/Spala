import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

import com.typesafe.sbt.SbtStartScript

object SpalaBuild extends Build {
  def extraResolvers = Seq(
    resolvers ++= Seq(
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + "/.m2/repository",
      Resolver.url("emchristiansen-scalatest-extra", url("https://raw.github.com/emchristiansen/scalatest-extra/master/releases"))( Patterns("[organisation]/[module]/[revision]/[artifact]-[revision].[ext]") )
    )
  )

 val publishSettings = Seq(
    organization := "emchristiansen",
    publishMavenStyle := false,
    publishTo := Some(Resolver.file("file", new File("./releases"))),
    version := "0.1-SNAPSHOT")

  val scalaVersionString = "2.10.2"

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersionString,
      "org.scala-lang" % "scala-compiler" % scalaVersionString,
      "org.apache.commons" % "commons-math3" % "3.2",
      "commons-io" % "commons-io" % "2.4",
      "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
      "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
      "org.scala-stm" %% "scala-stm" % "0.7",
      "com.chuusai" %% "shapeless" % "1.2.4",
      "org.clapper" %% "grizzled-scala" % "1.1.4",
      "org.scalanlp" %% "breeze-math" % "0.4-SNAPSHOT",
      "org.spire-math" %% "spire" % "0.5.0",
      "org.scalaz" %% "scalaz-core" % "7.0.2",
      "org.rogach" %% "scallop" % "0.9.3",
      "junit" % "junit" % "4.11" % "test",
      "emchristiansen" %% "scalatest-extra" % "0.1-SNAPSHOT",
      "com.sksamuel.scrimage" % "scrimage-core" % "1.3.3"
    )
  )

  def updateOnDependencyChange = Seq(
    watchSources <++= (managedClasspath in Test) map { cp => cp.files })

  def scalaSettings = Seq(
    scalaVersion := scalaVersionString,
    scalacOptions ++= Seq(
      "-optimize",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:implicitConversions",
      // "-language:reflectiveCalls",
      "-language:postfixOps"
    )
  )

  def moreSettings =
    Project.defaultSettings ++
    extraResolvers ++
    extraLibraryDependencies ++
    scalaSettings ++
    assemblySettings ++
    SbtStartScript.startScriptForJarSettings ++
    updateOnDependencyChange ++
    publishSettings

  val projectName = "Spala"
  lazy val root = {
    val settings = moreSettings ++ Seq(name := projectName, fork := true)
    Project(id = projectName, base = file("."), settings = settings)
  }
}
