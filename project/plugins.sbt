ThisBuild / scalaVersion := "2.12.12"
ThisBuild / autoStartServer := false

addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.13.1")
addSbtPlugin("org.scalameta"            % "sbt-scalafmt"        % "2.4.2")
addSbtPlugin("com.timushev.sbt"         % "sbt-updates"         % "0.5.1")
addSbtPlugin("io.spray"                 % "sbt-revolver"        % "0.9.1")
