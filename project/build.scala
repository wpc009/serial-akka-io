import sbtbuildinfo.BuildInfoPlugin._
import sbtbuildinfo.{BuildInfoKeys, BuildInfoOption, BuildInfoPlugin}
import com.typesafe.sbt.GitBranchPrompt
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.{GitKeys, git}
import com.typesafe.sbt.git.DefaultReadableGit
import sbt.{Credentials, _}
import sbt.Keys._

object SegmetricsBuild extends Build {

  val akka = "2.3.10"

  val common_settings = Seq(
    name := "serial-akka-io",
    organization := "com.segmetics",
    scalaVersion in ThisBuild := "2.11.6",
    exportJars := true,
    scalacOptions ++= Seq("-target:jvm-1.7", "-feature", "-g:none", "-optimise"),
    git.useGitDescribe := true,
    GitKeys.gitReader in ThisBuild <<= baseDirectory(base => new DefaultReadableGit(base)),
    sourceManaged <<= (baseDirectory in ThisProject) (base => base / "src_managed"),
    BuildInfoKeys.buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, BuildInfoKeys.buildInfoBuildNumber),
    BuildInfoKeys.buildInfoOptions += BuildInfoOption.BuildTime,
    BuildInfoKeys.buildInfoPackage := organization.value + ".buildinfo." + name.value.replaceAll("[-_.]", ""),
    resolvers ++= Seq(
      "labs repository" at "http://www.sparetimelabs.com/maven2",
      "snapshots" at "http://artifactory.segmetics.com/artifactory/libs-snapshot-local",
      "releases" at "http://artifactory.segmetics.com/artifactory/libs-release-local"
    ),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akka % Provided,
      "com.typesafe.akka" %% "akka-slf4j" % akka % Provided,
      "ch.qos.logback" % "logback-classic" % "1.0.13" % Provided,
      "com.sparetimelabs" % "purejavacomm" % "seg_0.0.23",
      "net.java.dev.jna" % "jna" % "4.1.0",
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

  lazy val io = Project(
    id = "serial-akka-io",
    base = file("io")
  ).enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
    .settings(common_settings: _*)
    .settings(publish_settings: _*)

  lazy val codec = Project(
    id = "serial-akka-codec",
    base = file("codec")
  ).enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
    .dependsOn(io)
    .settings(common_settings: _*)
    .settings(publish_settings: _*)
    .settings(Seq(
      name := "serial-akka-codec",
      libraryDependencies ++= Seq(
        "io.netty" % "netty-buffer" % "4.1.6.Final" % Provided
      )
    ))

}
