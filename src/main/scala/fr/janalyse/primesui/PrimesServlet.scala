package fr.janalyse.primesui

import org.scalatra._
import org.scalatra.scalate._
import javax.sql.DataSource
import javax.naming.InitialContext
import org.squeryl.SessionFactory
import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter
import javax.servlet.ServletRequest
import fr.janalyse.unittools._

case class PrimesUIContext(
  homeUrl: String,
  checkUrl: String,
  factorsUrl: String,
  primeUrl: String,
  primesUrl: String,
  populateUrl: String,
  ulamUrl: String,
  highcpucheckUrl:String,
  slowcheckUrl: String,
  slowsqlUrl: String,
  leakedcheckUrl: String,
  sessionleakedcheckUrl: String,
  jdbcleakcheckUrl: String,
  bigUrl: String,
  aliveUrl: String,
  sysinfoUrl: String,
  configUrl: String)

class PrimesServlet extends PrimesscalatraappStack with SysInfo {
  private val logger = org.slf4j.LoggerFactory.getLogger("fr.janalyse.primesui.PrimesServlet")

  override def isDevelopmentMode = false

  implicit class PrimesEngineRequest(request: ServletRequest) {
    def engine: PrimesEngine = request.getServletContext().getAttribute(PrimesEngine.KEY).asInstanceOf[PrimesEngine]
    def dbpool: Option[DataSource] = request.getServletContext().getAttribute(PrimesDBInit.KEY).asInstanceOf[Option[DataSource]]
  }

  val rnd = scala.util.Random
  def nextInt = synchronized { rnd.nextInt(10000) }

  lazy val ctx = PrimesUIContext(
    homeUrl = homeUrl,
    checkUrl = url("/check"),
    factorsUrl = url("/factors"),
    primeUrl = url("/prime"),
    primesUrl = url("/primes"),
    populateUrl = url("/populate"),
    ulamUrl = url("/ulam"),
    highcpucheckUrl = url("/highcpucheck"),
    slowcheckUrl = url("/slowcheck"),
    slowsqlUrl = url("/slowsql"),
    leakedcheckUrl = url("/leakedcheck"),
    sessionleakedcheckUrl = url("/sessionleakedcheck"),
    jdbcleakcheckUrl = url("/jdbcleakcheck"),
    bigUrl = url("/big"),
    aliveUrl = url("/alive"),
    sysinfoUrl = url("/sysinfo"),
    configUrl = url("/config"))

  import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

  def gotoUrl(goto: Option[String])(implicit request: HttpServletRequest, response: HttpServletResponse) =
    for { u <- goto } yield url(u, includeServletPath = false)

  def again(target: Option[String])(implicit request: HttpServletRequest, response: HttpServletResponse) =
    for { ref <- gotoUrl(target).toSeq } yield <i><a href={ ref }>Again</a></i>

  def homeUrl(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    url("/.", includeServletPath = false)
  }

  private def forTestingPurposesOnly(proc: => play.twirl.api.Html): play.twirl.api.Html = {
    if (request.engine.useTesting) proc else html.disabledFeature.render(ctx)
  }

  // ---------------------------------------------------------------------------------------------------------

  before("*") {
    response.setHeader("Cache-control", "no-cache, no-store, max-age=0, no-transform")

  }

  // ---------------------------------------------------------------------------------------------------------

  error {
    case e: Throwable => {
      val rq = request.pathInfo
      logger.error(s"Internal exception while processing $rq", e)
      html.error.render(ctx)
    }
  }

  // ---------------------------------------------------------------------------------------------------------

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
    html.index.render(ctx, request.engine, count, MetaInfo.version)
  }

  // ---------------------------------------------------------------------------------------------------------

  def check(num: Long, againUrl: Option[String]) = {
    val engine = request.engine
    val value = engine.check(num)
    contentType = "text/html"
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl), None)
  }

  get("/check/:num") {
    val num = params("num").toLong
    check(num, None)
  }
  get("/check") {
    check(nextInt, Some("/check"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def slowcheck(num: Long, delay: String = "1s", againUrl: Option[String] = None) = forTestingPurposesOnly {
    val engine = request.engine
    Thread.sleep(delay.toDuration())
    val value = engine.check(num)
    contentType = "text/html"
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),
      Some(s"This page simulates a slow application server with a minimum response time of $delay."))
  }

  get("/slowcheck/:num/:delay") {
    val delay = params.get("delay").getOrElse("1s")
    val num = params("num").toLong
    slowcheck(num, delay)
  }
  get("/slowcheck/:num") {
    val num = params("num").toLong
    slowcheck(num)
  }

  get("/slowcheck") {
    slowcheck(nextInt, againUrl = Some("/slowcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def highcpucheck(num: Long, againUrl: Option[String] = None) = forTestingPurposesOnly {
    val engine = request.engine
    val pgen = new fr.janalyse.primes.PrimesGenerator[Long]
    val value = pgen.checkedValues.find(v => v.value == num)
    contentType = "text/html"
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),
      Some(
        s"This page has a high CPU impact on the server, " +
          "all values from 1 to $num are tested, because we " +
          "need to give the prime or not prime rank of $num."))
  }

  get("/highcpucheck/:num") {
    val num = params("num").toLong
    highcpucheck(num)
  }

  get("/highcpucheck") {
    highcpucheck(nextInt, againUrl = Some("/slowcheck"))
  }
  // ---------------------------------------------------------------------------------------------------------

  def slowsql(num: Long, secs: Long = 1L, againUrl: Option[String] = None) = forTestingPurposesOnly {
    val engine = request.engine
    val dbpool = request.dbpool
    val value = engine.slowsqlcheck(num, dbpool, secs)
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),
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
    slowsql(nextInt, againUrl = Some("/slowsql"))
  }

  // ---------------------------------------------------------------------------------------------------------

  var leak = List.empty[Array[Byte]]

  def leakedcheck(num: Long, howmany: String = "1mb", againUrl: Option[String] = None) = forTestingPurposesOnly {
    val engine = request.engine
    leak = (Array.fill[Byte](howmany.toSize().toInt)(0x1)) :: leak
    val value = engine.check(num)
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),
      Some(s"this page simulates a memory leak, you've just lost $howmany of heap memory."))
  }

  get("/leakedcheck/:num/:howmany") {
    val howmany = params.get("howmany").getOrElse("1mb")
    val num = params("num").toLong
    leakedcheck(num, howmany)
  }

  get("/leakedcheck/:num") {
    val num = params("num").toLong
    leakedcheck(num)
  }

  get("/leakedcheck") {
    leakedcheck(nextInt, againUrl = Some("/leakedcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def sessionleakedcheck(num: Long, howmany: String = "1mb", againUrl: Option[String] = None) = forTestingPurposesOnly {
    val engine = request.engine
    val newleak = (Array.fill[Byte](howmany.toSize().toInt)(0x1))
    val sessionleaks = Option(request.getSession.getAttribute("sessionmemleaks")).map(_.asInstanceOf[List[Array[Byte]]]) match {
      case None        => List(newleak)
      case Some(leaks) => newleak :: leaks
    }
    request.getSession.setAttribute("sessionmemleaks", sessionleaks)
    val value = engine.check(num)
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),
      Some(s"this page simulates a session memory leak, you've just lost $howmany of heap memory in this current session."))
  }

  get("/sessionleakedcheck/:num/:howmany") {
    val howmany = params.get("howmany").getOrElse("1mb")
    val num = params("num").toLong
    sessionleakedcheck(num, howmany)
  }

  get("/sessionleakedcheck/:num") {
    val num = params("num").toLong
    sessionleakedcheck(num)
  }

  get("/sessionleakedcheck") {
    sessionleakedcheck(nextInt, againUrl = Some("/sessionleakedcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def jdbcleakcheck(num: Long, againUrl: Option[String] = None) = forTestingPurposesOnly {
    val engine = request.engine
    val value = engine.check(num)
    val dbpool = request.dbpool
    dbpool.map(ds => ds.getConnection) // oups one connection lost
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),
      Some(s"this page simulates a jdbc connection leak, you've just lost one !"))
  }

  get("/jdbcleakcheck/:num") {
    val num = params("num").toLong
    jdbcleakcheck(num)
  }

  get("/jdbcleakcheck") {
    jdbcleakcheck(nextInt, againUrl = Some("/jdbcleakcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def prime(nth: Long, againUrl: Option[String]) = {
    val engine = request.engine
    val checked = engine.getPrime(nth)
    html.primeResult.render(ctx, nth, checked, gotoUrl(againUrl))
  }

  get("/prime/:nth") {
    val nth = params("nth").toLong
    prime(nth, None)
  }

  get("/prime") {
    prime(nextInt, Some("/prime"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def primes(below: Long, above: Long = 0L) = {
    val engine = request.engine
    val primes = engine.listPrimes(below, above)
    html.primesList(ctx, below, above, primes)
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

  // ---------------------------------------------------------------------------------------------------------

  def factors(num: Long, againUrl: Option[String] = None) = {
    val engine = request.engine
    val factors = engine.factorize(num)
    html.factors.render(ctx, num, factors, gotoUrl(againUrl))
  }

  get("/factors/:num") {
    val num = params("num").toLong
    factors(num)
  }

  get("/factors") {
    factors(nextInt, Some("/factors"))
  }

  // ---------------------------------------------------------------------------------------------------------

  get("/populate/:upto") {
    val uptoAsked = params("upto").toLong
    val limit = 2000000L
    val (upto, msg) =
      if (request.engine.useTesting) {
        uptoAsked -> None
      } else {
        math.min(limit, uptoAsked) -> Some(s"$uptoAsked asked, authorized maximum is $limit !")
      }
    val engine = request.engine

    html.populate.render(ctx, request.engine, upto, msg)
  }

  // ---------------------------------------------------------------------------------------------------------

  get("/ulam/:sz") {
    val engine = request.engine
    val sz = params.get("sz").map(_.toInt).getOrElse(100)
    val bytes = engine.ulamAsPNG(sz)
    contentType = "image/png"
    response.getOutputStream().write(bytes)
  }

  // ---------------------------------------------------------------------------------------------------------

  def big(howmanyKB: Int = 3 * 1024) = forTestingPurposesOnly {
    html.big.render(ctx, howmanyKB)
  }

  get("/big/:howmany") {
    val howmanyKB = params.get("howmany").map(_.toInt).getOrElse(3 * 1024)
    big(howmanyKB)
  }

  get("/big") {
    big()
  }

  // ---------------------------------------------------------------------------------------------------------

  get("/config") {
    forTestingPurposesOnly {
      html.config.render(ctx, request.engine)
    }
  }

  post("/config") {
    forTestingPurposesOnly {
      val engine = request.engine
      params.get("usecache") match {
        case None => engine.setUseCache(false)
        case _    => engine.setUseCache(true)
      }
      redirect("/")
    }
  }

  // ---------------------------------------------------------------------------------------------------------

  get("/alive") {
    response.setContentType("text/plain")
    "OK"
  }

  // ---------------------------------------------------------------------------------------------------------

  get("/sysinfo") {
    response.setContentType("text/plain")
    if (request.engine.useTesting) {
      sysinfoProps.toList.sorted.map { case (k, v) => k + "=" + v }.mkString("\n")
    } else {
      "extra.sysinfo=disabled"
    }
  }

}
