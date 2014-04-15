import sbt._
import sbt.Keys._
import scala.Some
import xerial.sbt.Pack._

object SegmetricsBuild extends Build {

  lazy val copyDependencies = TaskKey[Unit]("copy-deps")

  lazy val deployTask = InputKey[Unit]("deploy", "Deploy binary to server")

  case class Profile(identityFile: File, user: String, host: String, src: File, dst: File)


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

  val common_settings = Project.defaultSettings ++ Seq(
    name := "serial-akka-io",
    organization := "com.segmetics",
    version := "0.1.3",
    exportJars := true,
    scalacOptions ++= Seq("-target:jvm-1.7", "-feature", "-g:none", "-optimise"),
    //    resolvers += "secmon proxy" at "http://nexus.innoxyz.com/nexus/content/groups/ivyGroup/",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.2.3",
      "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
      "ch.qos.logback" % "logback-classic" % "1.0.13",
      "com.sparetimelabs" % "purejavacomm" % "0.0.21",
      "net.java.dev.jna" % "jna" % "4.1.0",
      "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
      "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test"
    )
  );

  val publish_settings = Seq(
    publishTo := Some("artifactory.segmetics.com-releases" at "http://artifactory.segmetics.com/artifactory/libs-release-local"),
    credentials += Credentials("Artifactory Realm","artifactory.segmetics.com","deploy","5jtuDeploy")
  )

  lazy val project = Project(
    id = "serial-akka-io",
    base = file("."),
    settings = common_settings ++ packSettings ++ publish_settings
  )

}