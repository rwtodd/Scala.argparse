import Dependencies._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version      := "1.0"
ThisBuild / organization := "org.rwtodd.argparse"

lazy val root = (project in file("."))
	.settings(
		scalacOptions ++= Seq("-target:11", "-deprecation"),
		name := "argparse",
		libraryDependencies += scalaTest % Test
	)

// vim: filetype=sbt:noet:tabstop=4:autoindent
