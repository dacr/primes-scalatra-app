import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._

object PrimesscalatraappBuild extends Build {
  val Organization = "fr.janalyse"
  val Name = "primesui"
  val Version = "0.1.1"
  val ScalaVersion = "2.11.5"
  val ScalatraVersion = "2.3.0"

  lazy val project = Project (
    "primes-scalatra-app",
    file("."),
    settings = Defaults.defaultSettings ++ ScalatraPlugin.scalatraWithJRebel ++ /*scalateSettings ++ */ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "sonatype repository" at "https://oss.sonatype.org/content/repositories/releases/",
      resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/",
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
        "fr.janalyse" %% "primes" % "1.2.1",
        "fr.janalyse" %% "janalyse-jmx" % "0.7.1",
        "org.squeryl" %% "squeryl" % "0.9.5-7",
        "com.mchange" % "c3p0" % "0.9.5",
        "net.sf.ehcache" % "ehcache-core" % "2.6.10",
        "javax.transaction" % "jta" % "1.1", // required for ehcache
        "mysql" % "mysql-connector-java" % "5.1.34",
        "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.16.v20140903" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet.jsp" % "2.2.0.v201112011158" % "container;provided;test",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
      ).map(
          _.exclude("org.scala-lang", "scala-compiler")
           .exclude("org.scala-lang", "scala-reflect")
           .exclude("com.typesafe.akka", "akka-actor_2.11")
	         .exclude("org.scala-lang", "jline")
            )
      
    )
  )
}
