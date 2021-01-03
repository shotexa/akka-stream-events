import sbt._

object Dependencies {

  object Test {
    object org {
      object scalatest {
        val scalatest = "org.scalatest" %% "scalatest" % "3.2.3"
      }

      object scalacheck {
        val scalacheck = "org.scalacheck" %% "scalacheck" % "1.15.1",
      }

      object scalatestplus {
        val `scalacheck-1-14` =
          "org.scalatestplus" %% "scalacheck-1-14" % "3.2.2.0"
      }
    }
  }

  object Core {
    object com {
      object lihaoyi {
        val `os-lib` = "com.lihaoyi" %% "os-lib" % "0.7.1"
      }
    }
  }
}
