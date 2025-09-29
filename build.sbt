import sbtcrossproject.CrossProject

val Version = new {
  val Cats = "2.13.0"
  val Circe = "0.14.14"
  val Scala = "3.3.6"
}

def module(identifier: Option[String], jvmOnly: Boolean = false): CrossProject = {
  val platforms = JVMPlatform :: (if (jvmOnly) Nil else JSPlatform :: Nil)

  CrossProject(identifier.getOrElse("root"), file(identifier.fold(".")("modules/" + _)))(platforms: _*)
    .crossType(CrossType.Pure)
    .withoutSuffixFor(JVMPlatform)
    .build()
    .settings(
      Compile / scalacOptions ++= "-source:future" :: "-rewrite" :: "-new-syntax" :: "-Wunused:all" :: Nil,
      name := "data" + identifier.fold("")("-" + _)
    )
}

inThisBuild(
  Def.settings(
    developers := List(Developer("taig", "Niklas Klein", "mail@taig.io", url("https://taig.io/"))),
    dynverVTagPrefix := false,
    homepage := Some(url("https://github.com/taig/data/")),
    licenses := List("MIT" -> url("https://raw.githubusercontent.com/taig/data/main/LICENSE")),
    scalaVersion := Version.Scala,
    versionScheme := Some("early-semver")
  )
)

noPublishSettings

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
      "org.typelevel" %%% "cats-core" % Version.Cats ::
        Nil
  )

lazy val circe = module(identifier = Some("circe"))
  .settings(
    libraryDependencies ++=
      "io.circe" %%% "circe-core" % Version.Circe ::
        Nil
  )
  .dependsOn(core)
