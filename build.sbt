import sbt.Keys._

lazy val commonSettings = Seq(

  scalaVersion  := Versions.scala,

  scalacOptions ++= Seq("-deprecation", "-feature"),

  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-o", "-h", "target/testReports"),

  libraryDependencies ++= Seq(
    "com.faunadb"               %% "faunadb-scala"            % Versions.faunadb
  )
)


lazy val basics = (project in file("basics")).
  settings(commonSettings: _*).
  settings(
    name := "basics"
  )
