import sbt.Keys._

lazy val commonSettings = Seq(

  scalaVersion  := "2.11.11",

  scalacOptions ++= Seq("-deprecation", "-feature"),

  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-o", "-h", "target/testReports"),

  libraryDependencies ++= Seq(
    "com.faunadb"               %% "faunadb-scala"            % "1.1.0"
  )
)


lazy val basics = (project in file("basics")).
  settings(commonSettings: _*).
  settings(
    name := "basics"
  )
