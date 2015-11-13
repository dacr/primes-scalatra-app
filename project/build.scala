import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import play.twirl.sbt.SbtTwirl
//import com.earldouglas.xwp.JettyPlugin

object PrimesscalatraappBuild extends Build {

  def penvOrElse(key:String, alt:String):String = {
    import scala.util.Properties._
    propOrNone(key).orElse(envOrNone(key)).getOrElse(alt)
  }

  val Organization    = "fr.janalyse"
  val Name            = "primesui"
  val Version         = penvOrElse("PRIMESUI_REV", "0.1.27-SNAPSHOT")
  val PrimesVersion   = penvOrElse("PRIMES_REV",   "1.2.2-SNAPSHOT")
  val ScalaVersion    = "2.11.7"
  val ScalatraVersion = "2.4.0.RC3"
  //val ScalatraVersion = "2.4.0-SNAPSHOT"

  lazy val project = Project(
    "primes-scalatra-app",
    file("."),
    settings = ScalatraPlugin.scalatraSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
        artifact.name + "." + artifact.extension
      },
      parallelExecution in Test := false,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "sonatype repository" at "https://oss.sonatype.org/content/repositories/releases/",
      resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/",
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
        "fr.janalyse" %% "primes" % PrimesVersion,
        "fr.janalyse" %% "unittools" % "0.2.7-SNAPSHOT",
        "fr.janalyse" %% "janalyse-jmx" % "0.7.1",
        "org.squeryl" %% "squeryl" % "0.9.5-7",
        // problem with java7 it uses new java8 jdbc API
        // => java.lang.NoClassDefFoundError: java/sql/SQLType
        // https://github.com/swaldman/c3p0/issues/57
        //"com.mchange" % "c3p0" % "0.9.5.1",
        "com.mchange" % "c3p0" % "0.9.2.1",
        "net.sf.ehcache" % "ehcache-core" % "2.6.11",
        "javax.transaction" % "jta" % "1.1", // required for ehcache
        "mysql" % "mysql-connector-java" % "5.1.36",
        "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "9.2.10.v20150310" % "container",
        "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"
      ).map(
          _.exclude("org.scala-lang", "scala-compiler")
            .exclude("org.scala-lang", "scala-reflect")
            .exclude("com.typesafe.akka", "akka-actor_2.11")
            .exclude("com.typesafe.akka", "akka-stream-experimental_2.11")
            .exclude("org.scala-lang", "jline")
            )
    ) // SEQ End
  )//.enablePlugins(JettyPlugin)
   .enablePlugins(SbtTwirl)
  
}
