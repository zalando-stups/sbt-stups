package sbtstups

import sbt.Keys._
import sbt._

import scala.language.postfixOps
import sys.process.{Process, ProcessBuilder}

object SbtStupsPlugin extends AutoPlugin {
  object autoImport {
    lazy val createScmSource = taskKey[Unit]("Generate scm-source.json file")
    lazy val createKioVersion =
      taskKey[Unit]("Creates a new kio version of your application")
    lazy val pierOneLogin = taskKey[Unit]("Log into pierone")
    lazy val zawsLogin    = taskKey[Unit]("Log into zaws")

    lazy val scmSourceDirectory =
      settingKey[File]("Destination for scm-source.json output file")
    lazy val kioTeamName = settingKey[String]("Your Kio team name")
    lazy val kioApplicationName =
      settingKey[String]("Kio application name, defaults to name")
    lazy val kioApplicationVersion =
      settingKey[String]("Kio application version, defaults to version")
    lazy val zawsProfile =
      settingKey[String]("Your zaws login profile, defaults to kioTeamName")
    lazy val pierOneTeamName =
      settingKey[String]("Your pierone team name, defaults to kioTeamName")
    lazy val pierOneUrl =
      settingKey[String]("Your pierone repo url")
    lazy val dockerArtifactName =
      settingKey[String](
        "Your docker artifact name, defaults to Keys.normalizedName.value")
    lazy val dockerVersion =
      settingKey[String]("Your docker version, defaults to version")

    // Path prefix settings
    lazy val kioPathPrefix =
      settingKey[String]("Prefix for kio if you are using a custom path")
    lazy val pierOnePathPrefix =
      settingKey[String]("Prefix for pierOne if you are using a custom path")
    lazy val zawsPathPrefix =
      settingKey[String]("Prefix for zaws if you are using a custom path")
    lazy val gitPathPrefix =
      settingKey[String]("Git path prefix if you are using a custom path")

  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  private def cmd(input: String*): ProcessBuilder = Process(input)

  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Seq(
      scmSourceDirectory := baseDirectory.value,
      kioApplicationVersion := version.value,
      zawsProfile := kioTeamName.value,
      pierOneTeamName := kioTeamName.value,
      dockerArtifactName := Keys.normalizedName.value,
      kioApplicationName := name.value,
      dockerVersion := version.value,
      createScmSource := {
        streams.value.log.info("Creating scm-source.json")
        val rev =
          (cmd(s"${gitPathPrefix.value}git", "rev-parse", "HEAD") !!).trim
        val url =
          (cmd(s"${gitPathPrefix.value}git",
               "config",
               "--get",
               "remote.origin.url") !!).trim
        val status =
          (cmd(s"${gitPathPrefix.value}git", "status", "--porcelain") !!)
            .replaceAll("\n", "")
        val user = sys.env("USER").trim
        val finalRev = if (!status.isEmpty) {
          s"$rev (locally modified)"
        } else {
          rev
        }
        val outputJson =
          s"""{"url":"git:$url","revision":"$finalRev","author":"$user","status":"$status"}"""
        val file = scmSourceDirectory.value / "scm-source.json"
        IO.write(file, outputJson)
      },
      pierOneLogin := {
        streams.value.log.info("Logging into pierone")
        val pieroneLogin =
          cmd(s"${pierOnePathPrefix.value}pierone",
              "login",
              "--url",
              pierOneUrl.value) !

        if (pieroneLogin != 0)
          sys.error("Could not log into pierone")
      },
      zawsLogin := {
        val zawsLogin =
          cmd(s"${zawsPathPrefix.value}zaws", "login", zawsProfile.value) !

        if (zawsLogin != 0)
          sys.error("Could not log into zaws")
      },
      createKioVersion := {
        streams.value.log.info("Creating Kio application version")
        val publishToKio = cmd(
          s"${kioPathPrefix.value}kio",
          "versions",
          "create",
          kioApplicationName.value,
          kioApplicationVersion.value,
          s"${pierOneUrl.value}/${pierOneTeamName.value}/${dockerArtifactName.value}:${dockerVersion.value}"
        ) !

        if (publishToKio != 0)
          sys.error("Could not publish to Kio")
      },
      createKioVersion := (createKioVersion dependsOn zawsLogin).value,
      kioPathPrefix := "",
      pierOnePathPrefix := "",
      zawsPathPrefix := "",
      gitPathPrefix := ""
    )

}
