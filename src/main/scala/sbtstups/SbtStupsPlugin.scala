package sbtstups

import sbt.Keys._
import sbt._

import scala.language.postfixOps

object SbtStupsPlugin extends AutoPlugin {
  object autoImport {
    lazy val createScmSource = taskKey[Unit]("Generate scm-source.json file")
    lazy val createKioVersion =
      taskKey[Unit]("Creates a new kio version of your application")
    lazy val pierOneLogin = taskKey[Unit]("Log into pierone")
    lazy val maiLogin     = taskKey[Unit]("Log into mai")

    lazy val scmSourceDirectory =
      settingKey[File]("Destination for scm-source.json output file")
    lazy val kioTeamName = settingKey[String]("Your Kio team name")
    lazy val kioApplicationName =
      settingKey[String]("Kio application name, defaults to name")
    lazy val kioApplicationVersion =
      settingKey[String]("Kio application version, defaults to version")
    lazy val maiProfile =
      settingKey[String]("Your mai login profile, defaults to kioTeamName")
    lazy val pierOneTeamName =
      settingKey[String]("Your pierone team name, defaults to kioTeamName")
    lazy val pierOneUrl =
      settingKey[String]("Your pierone repo url")
    lazy val dockerArtifactName =
      settingKey[String](
        "Your docker artifact name, defaults to Keys.normalizedName.value")
    lazy val dockerVersion =
      settingKey[String]("Your docker version, defaults to version")
  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Seq(
      kioTeamName := sys.error(
        "A kioTeamName isn't defined. Please define one"),
      scmSourceDirectory := baseDirectory.value,
      kioApplicationVersion := version.value,
      maiProfile := kioTeamName.value,
      pierOneTeamName := kioTeamName.value,
      pierOneUrl := sys.error(
        "A pierOneUrl isn't defined. Please define one."),
      dockerArtifactName := Keys.normalizedName.value,
      kioApplicationName := name.value,
      dockerVersion := version.value,
      createScmSource := {
        streams.value.log.info("Creating scm-source.json")
        try {
          val rev = (List("git", "rev-parse", "HEAD") !!).trim
          val url =
            (List("git", "config", "--get", "remote.origin.url") !!).trim
          val status =
            (List("git", "status", "--porcelain") !!).replaceAll("\n", "")
          val user = sys.env("USER").trim
          val finalRev = if (!rev.isEmpty) {
            s"$rev (locally modified)"
          } else {
            rev
          }
          val outputJson =
            s"""{"url":"git:$url","revision":"$finalRev","author":"$user","status":"$status"}"""
          val file = scmSourceDirectory.value / "scm-source.json"
          IO.write(file, outputJson)
        } catch {
          case e: Throwable =>
            sys.error("Could not create scm-source.json")
        }
      },
      pierOneLogin := {
        streams.value.log.info("Logging into pierone")
        val pieroneLogin = List("pierone", "login") !

        if (pieroneLogin != 0)
          sys.error("Could not log into pierone")
      },
      maiLogin := {
        val maiLogin = List("mail", "login", maiProfile.value) !

        if (maiLogin != 0)
          sys.error("Could not log into mai")
      },
      createKioVersion := {
        streams.value.log.info("Creating Kio application version")
        val publishToKio = List(
          "kio",
          "versions",
          "create",
          kioApplicationName.value,
          kioApplicationVersion.value,
          s"${pierOneUrl.value}/${pierOneTeamName.value}/${dockerArtifactName.value}:${dockerVersion.value}"
        ) !

        if (publishToKio != 0)
          sys.error("Could not publish to Kio")
      },
      createKioVersion <<= createKioVersion dependsOn maiLogin
    )

}
