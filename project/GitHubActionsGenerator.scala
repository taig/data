import io.circe.Json
import io.circe.syntax._

object GitHubActionsGenerator {
  object Step {
    val SetupJava: Json = Json.obj(
      "name" := "Setup Java",
      "uses" := "actions/setup-java@v4",
      "with" := Json.obj(
        "cache" := "sbt",
        "distribution" := "temurin",
        "java-version" := "21"
      )
    )

    val SetupSbt: Json = Json.obj(
      "name" := "Setup sbt",
      "uses" := "sbt/setup-sbt@v1"
    )

    val Checkout: Json = Json.obj(
      "name" := "Checkout",
      "uses" := "actions/checkout@v4",
      "with" := Json.obj(
        "fetch-depth" := 0
      )
    )
  }

  object Job {
    def apply(name: String, mode: String = "DEV", needs: List[String] = Nil)(steps: Json*): Json = Json.obj(
      "name" := name,
      "runs-on" := "ubuntu-latest",
      "needs" := needs,
      "env" := Json.obj(
        s"SBT_TPOLECAT_$mode" := "true"
      ),
      "steps" := steps
    )

    val Blowout: Json = Job(name = "Blowout")(
      Step.Checkout,
      Step.SetupJava,
      Step.SetupSbt,
      Json.obj("run" := "sbt blowoutCheck")
    )

    val Scalafmt: Json = Job(name = "Scalafmt")(
      Step.Checkout,
      Step.SetupJava,
      Step.SetupSbt,
      Json.obj("run" := "sbt scalafmtCheckAll")
    )

    val Scalafix: Json = Job(name = "Scalafix", mode = "CI")(
      Step.Checkout,
      Step.SetupJava,
      Step.SetupSbt,
      Json.obj("run" := "sbt scalafixCheckAll")
    )

    val Deploy: Json =
      Job(name = "Deploy", mode = "RELEASE", needs = List("blowout", "scalafmt", "scalafix"))(
        Step.Checkout,
        Step.SetupJava,
        Step.SetupSbt,
        Json.obj(
          "name" := "Release",
          "run" := "sbt ci-release",
          "env" := Json.obj(
            "PGP_PASSPHRASE" := "${{secrets.PGP_PASSPHRASE}}",
            "PGP_SECRET" := "${{secrets.PGP_SECRET}}",
            "SONATYPE_PASSWORD" := "${{secrets.SONATYPE_PASSWORD}}",
            "SONATYPE_USERNAME" := "${{secrets.SONATYPE_USERNAME}}"
          )
        )
      )
  }

  val main: Json = Json.obj(
    "name" := "CI",
    "on" := Json.obj(
      "push" := Json.obj("branches" := List("main"))
    ),
    "jobs" := Json.obj(
      "blowout" := Job.Blowout,
      "scalafix" := Job.Scalafix,
      "scalafmt" := Job.Scalafmt,
      "deploy" := Job.Deploy
    )
  )

  val tag: Json = Json.obj(
    "name" := "CD",
    "on" := Json.obj(
      "push" := Json.obj("tags" := List("*.*.*"))
    ),
    "jobs" := Json.obj(
      "blowout" := Job.Blowout,
      "scalafix" := Job.Scalafix,
      "scalafmt" := Job.Scalafmt,
      "deploy" := Job.Deploy
    )
  )

  val pullRequest: Json = Json.obj(
    "name" := "CI",
    "on" := Json.obj(
      "pull_request" := Json.obj(
        "branches" := List("main")
      )
    ),
    "jobs" := Json.obj(
      "blowout" := Job.Blowout,
      "scalafix" := Job.Scalafix,
      "scalafmt" := Job.Scalafmt
    )
  )
}
