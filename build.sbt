val Http4sVersion = "0.21.11"
val CirceVersion = "0.13.0"
val Specs2Version = "4.10.5"
val LogbackVersion = "1.2.3"
val EnumeratumVersion = "1.6.1"
val DoobieVersion = "0.9.0"
val CaseAppVersion = "2.0.4"
val OdinVersion = "0.9.1"

lazy val root = (project in file("."))
  .settings(
    organization := "io.lenses",
    name := "buildmetrics",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.3",
    scalacOptions := Seq(
      "-encoding",
      "utf8", // Option and arguments on same line
      //"-Xfatal-warnings", // New lines for each options
      "-Ywarn-unused",
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps",
      "-Ymacro-annotations"
    ),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      // "io.circe" %% "circe-generic-extras" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "com.beachape" %% "enumeratum" % EnumeratumVersion,
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "com.github.alexarchambault" %% "case-app" % CaseAppVersion,
      "com.github.alexarchambault" %% "case-app-cats" % CaseAppVersion,
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "org.tpolecat" %% "doobie-specs2" % DoobieVersion % "test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.github.valskalla" %% "odin-core" % OdinVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
