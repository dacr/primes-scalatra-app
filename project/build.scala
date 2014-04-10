import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
//import com.mojolly.scalate.ScalatePlugin._
//import ScalateKeys._

object PrimesscalatraappBuild extends Build {
  val Organization = "fr.janalyse"
  val Name = "primesui"
  val Version = "0.1.0"
  val ScalaVersion = "2.10.3"
  val ScalatraVersion = "2.2.2"

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
        //"org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        //"com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "org.scalatra" %% "scalatra-scalatest" % "2.2.2" % "test",
        "fr.janalyse" %% "primes" % "1.0.9",
        "fr.janalyse" %% "janalyse-jmx" % "0.6.3",
        "org.squeryl" %% "squeryl" % "0.9.5-6",
        "com.mchange" % "c3p0" % "0.9.5-pre8",
        "net.sf.ehcache" % "ehcache-core" % "2.6.8",
        "javax.transaction" % "jta" % "1.1", // required for ehcache
        "mysql" % "mysql-connector-java" % "5.1.28",
        "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
      ).map(
          _.exclude("org.scala-lang", "scala-compiler")
           .exclude("org.scala-lang", "scala-reflect")
           .exclude("com.typesafe.akka", "akka-actor_2.10")
            )
      
      /*,
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile){ base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      }
      */
    )
  )
}
