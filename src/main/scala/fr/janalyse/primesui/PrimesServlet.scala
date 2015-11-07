package fr.janalyse.primesui

import org.scalatra._
import org.scalatra.scalate._
import javax.sql.DataSource
import javax.naming.InitialContext
import org.squeryl.SessionFactory
import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter
import javax.servlet.ServletRequest

case class PrimesUrls(
  homeUrl:String,
  checkUrl: String,
  factorsUrl: String,
  primeUrl: String,
  primesUrl: String,
  populateUrl: String,
  ulamUrl: String,
  slowcheckUrl: String,
  slowsqlUrl: String,
  leakedcheckUrl: String,
  bigUrl: String,
  aliveUrl: String,
  sysinfoUrl: String,
  configUrl: String)

class PrimesServlet extends PrimesscalatraappStack {

  override def isDevelopmentMode = false

  implicit class PrimesEngineRequest(request: ServletRequest) {
    def engine: PrimesEngine = request.getServletContext().getAttribute(PrimesEngine.KEY).asInstanceOf[PrimesEngine]
    def dbpool: Option[DataSource] = request.getServletContext().getAttribute(PrimesDBInit.KEY).asInstanceOf[Option[DataSource]]
  }

  val rnd = scala.util.Random
  def nextInt = synchronized { rnd.nextInt(10000) }

  before("*") {
    response.setHeader("Cache-control", "no-cache, no-store, max-age=0, no-transform")

  }

  lazy val purls = PrimesUrls(
    homeUrl = homeUrl,
    checkUrl=url("/check"),
    factorsUrl=url("/factors"),
    primeUrl=url("/prime"),
    primesUrl=url("/primes"),
    populateUrl=url("/populate"),
    ulamUrl=url("/ulam"),
    slowcheckUrl=url("/slowcheck"),
    slowsqlUrl=url("/slowsql"),
    leakedcheckUrl=url("/leakedcheck"),
    bigUrl=url("/big"),
    aliveUrl=url("/alive"),
    sysinfoUrl=url("/sysinfo"),
    configUrl=url("/config")
  )
  
  get("/") {
    val count = if (!request.engine.useSession) None else {
      val newcount = Option(request.getSession.getAttribute("count")).map(_.asInstanceOf[Long]) match {
        case None        => 1L
        case Some(count) => count + 1
      }
      request.getSession.setAttribute("count", newcount)
      Some(newcount)
    }
    contentType = "text/html"
    html.index.render(request.engine, purls, count, MetaInfo.version)
  }

  import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

  def gotoUrl(goto: Option[String])(implicit request: HttpServletRequest, response: HttpServletResponse) =
    for { u <- goto } yield url(u, includeServletPath = false)

  def again(target: Option[String])(implicit request: HttpServletRequest, response: HttpServletResponse) =
    for { ref <- gotoUrl(target).toSeq } yield <i><a href={ ref }>Again</a></i>

  def homeUrl(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    url("/.", includeServletPath = false)
  }
  def gotoMenu(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    <i><a href={ homeUrl }>Back to the menu</a></i>
  }

  def check(num: Long, againUrl: Option[String]) = {
    val engine = request.engine
    val value = engine.check(num)
    contentType = "text/html"
    html.checkResult.render(num, value, gotoUrl(againUrl), homeUrl, None)
  }

  get("/check/:num") {
    val num = params("num").toLong
    check(num, None)
  }
  get("/check") {
    check(nextInt, Some("/check"))
  }

  
  private def forTestingPurposesOnly(proc: => play.twirl.api.Html): play.twirl.api.Html = {
    if (request.engine.useTesting) proc else html.disabledFeature.render(homeUrl)
  }

  private def forTestingOnly(proc: => xml.NodeSeq): xml.NodeSeq = {
    if (request.engine.useTesting) proc else {
      <html>
        <body>
          <h1>Feature disabled...</h1>
          <p>{ gotoMenu }</p>
        </body>
      </html>
    }
  }

  def slowcheck(num: Long, secs: Long = 1L, againUrl: Option[String]=None) = forTestingPurposesOnly {
    val engine = request.engine
    Thread.sleep(secs * 1000L)
    val value = engine.check(num)
    contentType = "text/html"
    html.checkResult.render(num, value, gotoUrl(againUrl), homeUrl,
        Some(s"This page simulates a slow application server with a minimum response time of $secs second(s)."))
  }

  get("/slowcheck/:num/:secs") {
    val secs = params.get("secs").map(_.toLong).getOrElse(1L)
    val num = params("num").toLong
    slowcheck(num, secs)
  }
  get("/slowcheck/:num") {
    val num = params("num").toLong
    slowcheck(num)
  }

  get("/slowcheck") {
    slowcheck(nextInt, againUrl=Some("/slowcheck"))
  }

  def slowsql(num: Long, secs: Long = 1L, againUrl: Option[String]=None) = forTestingPurposesOnly {
    val engine = request.engine
    val dbpool = request.dbpool
    val value = engine.slowsqlcheck(num, dbpool, secs)
    html.checkResult.render(num, value, gotoUrl(againUrl), homeUrl,
        Some(s"This page simulates a slow database with a minimum response time of $secs second(s)."))
  }

  get("/slowsql/:num/:secs") {
    val secs = params.get("secs").map(_.toLong).getOrElse(1L)
    val num = params("num").toLong
    slowsql(num, secs)
  }
  get("/slowsql/:num") {
    val num = params("num").toLong
    slowsql(num)
  }

  get("/slowsql") {
    slowsql(nextInt, againUrl=Some("/slowsql"))
  }

  var leak = List.empty[Array[Byte]]

  def leakedcheck(num: Long, howmany: Int = 1, againUrl: Option[String]=None) = forTestingPurposesOnly {
    val engine = request.engine
    leak = (Array.fill[Byte](1024 * 1024 * howmany)(0x1)) :: leak
    val value = engine.check(num)
    html.checkResult.render(num, value, gotoUrl(againUrl), homeUrl,
        Some(s"this page simulates a memory leak, you've just lost $howmany megabytes"))
  }

  get("/leakedcheck/:num/:howmany") {
    val howmany = params.get("howmany").map(_.toInt).getOrElse(1)
    val num = params("num").toLong
    leakedcheck(num, howmany)
  }

  get("/leakedcheck/:num") {
    val num = params("num").toLong
    leakedcheck(num)
  }

  get("/leakedcheck") {
    leakedcheck(nextInt, againUrl=Some("/leakedcheck"))
  }

  def prime(nth: Long, againUrl: Option[String]) = {
    val engine = request.engine
    val checked = engine.getPrime(nth)
    html.primeResult.render(nth, checked, gotoUrl(againUrl), homeUrl)
  }

  get("/prime/:nth") {
    val nth = params("nth").toLong
    prime(nth, None)
  }

  get("/prime") {
    prime(nextInt, Some("/prime"))
  }

  def primes(below: Long, above: Long = 0L) = {
    val engine = request.engine
    val primes = engine.listPrimes(below, above)
    <html>
      <body>
        <h1>Primes number above { above } and below { below }</h1>
        <ul>
          {
            for { prime <- primes } yield {
              <li><pre>{ prime.nth } --> { prime.value }</pre></li>
            }
          }
        </ul>
        <p>{ gotoMenu }</p>
      </body>
    </html>
  }

  get("/primes/:below/:above") {
    val below = params.get("below").map(_.toLong).getOrElse(10000L)
    val above = params.get("above").map(_.toLong).getOrElse(0L)
    primes(below, above)
  }

  get("/primes/:below") {
    val below = params.get("below").map(_.toLong).getOrElse(10000L)
    primes(below)
  }

  get("/factors/:num") {
    val engine = request.engine
    val num = params("num").toLong
    val factors = engine.factorize(num).get // TODO : DANGEROUS 
    <html>
      <body>
        {
          if (factors.isEmpty) <h1>{ num } = { num } <i>and is prime</i> </h1>
          else <h1>{ num } = { factors.mkString(" * ") }</h1>
        }
        <p>{ gotoMenu }</p>
      </body>
    </html>
  }

  get("/factors") {
    val engine = request.engine
    val num = nextInt
    val factors = engine.factorize(num).get // TODO : DANGEROUS 
    <html>
      <body>
        {
          if (factors.isEmpty) <h1>{ num } = { num } <i>and is prime</i> </h1>
          else <h1>{ num } = { factors.mkString(" * ") }</h1>
        }
        <p>
          <i><a href={ url("/factors") }>Again</a></i>
          -
          { gotoMenu }
        </p>
      </body>
    </html>
  }

  get("/populate/:upto") {
    val uptoAsked = params("upto").toLong
    val limit = 2000000L
    val (upto, msg) =
      if (request.engine.useTesting)
        uptoAsked -> None
      else
        math.min(limit, uptoAsked) -> Some(s"$uptoAsked asked, authorized maximum is $limit !")
    val engine = request.engine

    html.populate.render(request.engine, purls, upto, msg)
  }

  get("/ulam/:sz") {
    val engine = request.engine
    val sz = params.get("sz").map(_.toInt).getOrElse(100)
    val bytes = engine.ulamAsPNG(sz)
    contentType = "image/png"
    response.getOutputStream().write(bytes)
  }

  get("/config") {
    forTestingPurposesOnly {
      html.config.render(request.engine, purls)
    }
  }

  post("/config") {
    forTestingOnly {
      val engine = request.engine
      params.get("usecache") match {
        case None => engine.setUseCache(false)
        case _    => engine.setUseCache(true)
      }
      redirect("/")
    }
  }

  def big(howmanyKB: Int = 3 * 1024) = forTestingOnly {
    <html>
      <body>
        <h1>This is a big page, > 3Mb</h1>
        {
          for { _ <- 1 to 16 * howmanyKB } yield {
            <p>1234567891234567891234567890123456789012345678901234567</p>
          }
        }
        <p>{ gotoMenu }</p>
      </body>
    </html>
  }

  get("/big/:howmany") {
    val howmanyKB = params.get("howmany").map(_.toInt).getOrElse(3 * 1024)
    big(howmanyKB)
  }

  get("/big") {
    big()
  }

  get("/alive") {
    response.setContentType("text/plain")
    "OK"
  }

  val selectedProps = {
    import collection.JavaConversions._
    val res = List(
      "^os[.].*",
      "^java[.]vm[.].*",
      "^java[.]vendor[.].*",
      "^java[.]runtime[.].*").map(_.r)
    val props = System.getProperties.toMap
    props.filter {
      case (k, v) =>
        res.exists(_.findFirstIn(k).isDefined)
    }
  }

  val extendedProps = {
    import java.lang.management.ManagementFactory
    val os = ManagementFactory.getOperatingSystemMXBean()
    val rt = ManagementFactory.getRuntimeMXBean()
    Map(
      "extra.availableProcessors" -> java.lang.Runtime.getRuntime.availableProcessors.toString(),
      "extra.sysinfo" -> "enabled")
  }

  get("/sysinfo") {
    response.setContentType("text/plain")
    if (request.engine.useTesting) {
      (selectedProps ++ extendedProps).toList.sorted.map { case (k, v) => k + "=" + v }.mkString("\n")
    } else {
      "extra.sysinfo=disabled"
    }
  }

}
