lazy val akkaHttpVersion       = "10.1.12"
lazy val akkaVersion           = "2.6.8"
lazy val slickVersion          = "3.3.2"
lazy val akkaManagementVersion = "1.0.8"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "io.vaan.notz",
      scalaVersion := "2.13.1"
    )
  ),
  name := "notz",
  libraryDependencies ++= Seq(
      "com.typesafe.akka"             %% "akka-http"                    % akkaHttpVersion,
      "com.typesafe.akka"             %% "akka-http-spray-json"         % akkaHttpVersion,
      "com.typesafe.akka"             %% "akka-actor-typed"             % akkaVersion,
      "com.typesafe.akka"             %% "akka-stream"                  % akkaVersion,
      "com.typesafe.akka"             %% "akka-cluster-sharding-typed"  % akkaVersion,
      "com.typesafe.akka"             %% "akka-persistence-typed"       % akkaVersion,
      "com.typesafe.akka"             %% "akka-persistence-query"       % akkaVersion,
      "com.typesafe.akka"             %% "akka-persistence-cassandra"   % "1.0.1",
      "com.typesafe.akka"             %% "akka-serialization-jackson"   % akkaVersion,
      "com.lightbend.akka.management" %% "akka-management"              % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
      "ch.qos.logback"                 % "logback-classic"              % "1.2.3",
      "com.typesafe.akka"             %% "akka-http-testkit"            % akkaHttpVersion % Test,
      "com.typesafe.akka"             %% "akka-actor-testkit-typed"     % akkaVersion     % Test,
      "org.scalatest"                 %% "scalatest"                    % "3.0.8"         % Test
    )
)
