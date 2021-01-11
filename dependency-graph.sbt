import Util._

val AkkaVersion     = "2.6.8"
val AkkaHttpVersion = "10.2.2"

lazy val `bet-history` = project
  .in(file("."))
  .settings(
    libraryDependencies ++= appDependencies ++ baseDependencies
  )

lazy val coreAppDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream"          % AkkaVersion,
  "com.typesafe.akka" %% "akka-http"            % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed"     % AkkaVersion
)

lazy val testAppDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit"   % AkkaHttpVersion
).map(_ % Test)

lazy val appDependencies = coreAppDependencies ++ testAppDependencies

lazy val coreDependencies = Seq(
  Dependencies.Core.com.lihaoyi.`os-lib`
)

lazy val testDependencies = Seq(
  Dependencies.Test.org.scalatest.scalatest,
  Dependencies.Test.org.scalacheck.scalacheck,
  Dependencies.Test.org.scalatestplus.`scalacheck-1-14`
).map(_ % Test)

lazy val baseDependencies = coreDependencies ++ testDependencies
