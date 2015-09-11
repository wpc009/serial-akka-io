import sbtbuildinfo.BuildInfoPlugin._
import sbtbuildinfo.{BuildInfoOption, BuildInfoKeys, BuildInfoPlugin}
import com.typesafe.sbt.GitBranchPrompt
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.{GitKeys, git}
import com.typesafe.sbt.git.DefaultReadableGit
import sbt._
import sbt.Keys._

object SegmetricsBuild extends Build {

  lazy val copyDependencies = TaskKey[Unit]("copy-deps")

  lazy val deployTask = InputKey[Unit]("deploy", "Deploy binary to server")

  case class Profile(identityFile: File, user: String, host: String, src: File, dst: File)

  val akka = "2.3.10"

  def deploy(p: Profile, log: Logger) = {
    scp(log, p.identityFile, p.user, p.host, p.src, p.dst)
  }

  def ssh(log: Logger, identityFile: File, user: String, host: String, commands: String*) {
    val chain = commands(0) + commands.toList.tail.foldLeft("")(_ + " && " + _)
    log.info("Executing command chain '" + chain + "' on remote server " + host + "...")
    "ssh -i " + identityFile + " " + user + "@" + host + " " + chain ! log
  }

  def scp(log: Logger, identityFile: File, user: String, host: String, src: File, dst: File) {
    log.info("Copying file " + src + " to " + host + ":" + dst + "...")
    "scp -r -i " + identityFile + " " + src + " " + user + "@" + host + ":" + dst ! log
  }

  def copyDepTask = copyDependencies <<= (update, crossTarget, scalaVersion, mainClass in Runtime) map {
    (updateReport, out, scalaVer, main) =>
      val template =
        """#!/bin/sh
          |java -classpath "%s" %s "$@"
          | """.stripMargin
      val mainStr = main getOrElse sys.error("No main class")

      val classpath = updateReport.allFiles map {
        srcPath =>
          val destPath = out / "lib" / srcPath.getName
          IO.copyFile(srcPath, destPath, preserveLastModified = true)
          "./lib/" + srcPath.getName
      }

      val contents = template.format(classpath.mkString(","), mainStr)

      val runScript = (out / "run.sh")
      IO.write(runScript, contents)

  }


  val common_settings = Seq(
    name := "serial-akka-io",
    organization := "com.segmetics",
//    version := "0.2.2-SNAPSHOT",
    //    crossScalaVersions := Seq("2.10.5","2.11.6"),
    scalaVersion in ThisBuild := "2.11.6",
    exportJars := true,
    scalacOptions ++= Seq("-target:jvm-1.7", "-feature", "-g:none", "-optimise"),
    git.useGitDescribe := true,
    GitKeys.gitReader in ThisBuild <<= baseDirectory(base => new DefaultReadableGit(base)),
    sourceManaged <<= (baseDirectory in ThisProject) ( base => base / "src_managed" ),
    BuildInfoKeys.buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion,BuildInfoKeys.buildInfoBuildNumber),
    BuildInfoKeys.buildInfoOptions += BuildInfoOption.BuildTime,
    BuildInfoKeys.buildInfoPackage := organization.value + ".buildinfo." + name.value.replaceAll("[-_.]",""),
    //    resolvers += "secmon proxy" at "http://nexus.innoxyz.com/nexus/content/groups/ivyGroup/",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akka,
      "com.typesafe.akka" %% "akka-slf4j" % akka % Provided,
      "ch.qos.logback" % "logback-classic" % "1.0.13" % Provided,
      "com.maxtropy" %% "maxtropy-logging" % "0.2.2-Alpha" changing(),
      "com.sparetimelabs" % "purejavacomm" % "seg_0.0.23",
      "net.java.dev.jna" % "jna" % "4.1.0",
      //      "net.java.dev.jna" % "jna" % "4.0.0" % "provided",
      //      "net.java.dev.jna" % "jna-platform" % "4.0.0" % "provided",
      "com.typesafe.akka" %% "akka-testkit" % akka % Test,
      "org.scalatest" %% "scalatest" % "3.0.0-M8" % Test
    )
  )

  val publish_settings = Seq(
    publishTo := {
      if (isSnapshot.value)
        Some("snapshots" at "http://artifactory.segmetics.com/artifactory/libs-snapshot-local")
      else
        Some("artifactory.segmetics.com-releases" at "http://artifactory.segmetics.com/artifactory/libs-release-local")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )

  lazy val project = Project(
    id = "serial-akka-io",
    base = file(".")
  )
    .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
    .settings(common_settings: _*)
    .settings(publish_settings: _*)
  //    .enablePlugins(GitVersioning,GitBranchPromt)

}
