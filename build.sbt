import sbtcrossproject.CrossProject

def module(identifier: Option[String], jvmOnly: Boolean = false): CrossProject = {
  val platforms = JVMPlatform :: (if (jvmOnly) Nil else JSPlatform :: Nil)

  CrossProject(identifier.getOrElse("root"), file(identifier.fold(".")("modules/" + _)))(platforms *)
    .crossType(CrossType.Pure)
    .withoutSuffixFor(JVMPlatform)
    .build()
    .settings(
      Compile / scalacOptions ++= "-source:future" :: "-rewrite" :: "-new-syntax" :: "-Wunused:all" :: Nil,
      name := "data" + identifier.fold("")("-" + _),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
    )
}

inThisBuild(
  Def.settings(
    developers := List(Developer("taig", "Niklas Klein", "mail@taig.io", uri("https://taig.io/"))),
    dynverVTagPrefix := false,
    homepage := Some(uri("https://github.com/taig/data/")),
    licenses := List("MIT" -> uri("https://raw.githubusercontent.com/taig/data/main/LICENSE")),
    scalaVersion := Version.Scala,
    versionScheme := Some("early-semver")
  )
)

lazy val root = module(identifier = None)
  .enablePlugins(BlowoutYamlPlugin)
  .settings(noPublishSettings)
  .settings(
    blowoutGenerators ++= {
      val workflows = file(".github") / "workflows"
      BlowoutYamlGenerator.lzy(workflows / "main.yml", GitHubActionsGenerator.main) ::
        BlowoutYamlGenerator.lzy(workflows / "pull-request.yml", GitHubActionsGenerator.pullRequest) ::
        BlowoutYamlGenerator.lzy(workflows / "tag.yml", GitHubActionsGenerator.tag) ::
        Nil
    }
  )
  .aggregate(core, circe)

lazy val core = module(identifier = Some("core"))
  .settings(
    libraryDependencies ++=
      "org.typelevel" %% "cats-core" % Version.Cats ::
        "dev.zio" %% "zio-test" % Version.Zio % "test" ::
        "dev.zio" %% "zio-test-sbt" % Version.Zio % "test" ::
        Nil
  )

lazy val circe = module(identifier = Some("circe"))
  .settings(
    libraryDependencies ++=
      "io.circe" %% "circe-core" % Version.Circe ::
        Nil
  )
  .dependsOn(core % "compile->compile;test->test")
