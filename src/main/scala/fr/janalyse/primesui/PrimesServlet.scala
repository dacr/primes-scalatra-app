package fr.janalyse.primesui

import org.scalatra._
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
  issuecheckUrl:String,
  highcpucheckUrl:String,
  overcheckUrl:String,
  slowcheckUrl: String,
  slowsqlcheckUrl: String,
  leakedcheckUrl: String,
  sessionleakedcheckUrl: String,
  jdbcleakcheckUrl: String,
  toomanylogscheckUrl:String,
  badlogscheckUrl:String,
  goodlogscheckUrl:String,
  bigUrl: String,
  aliveUrl: String,
  sysinfoUrl: String,
  configUrl: String
  )

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
    issuecheckUrl = url("/issuecheck"),
    highcpucheckUrl = url("/highcpucheck"),
    overcheckUrl = url("/overcheck"),
    slowcheckUrl = url("/slowcheck"),
    slowsqlcheckUrl = url("/slowsqlcheck"),
    leakedcheckUrl = url("/leakedcheck"),
    sessionleakedcheckUrl = url("/sessionleakedcheck"),
    jdbcleakcheckUrl = url("/jdbcleakcheck"),
    toomanylogscheckUrl = url("/toomanylogscheck"),
    badlogscheckUrl = url("/badlogscheck"),
    goodlogscheckUrl = url("/goodlogscheck"),
    bigUrl = url("/big"),
    aliveUrl = url("/alive"),
    sysinfoUrl = url("/sysinfo"),
    configUrl = url("/config")
    )

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
  // in comment because it takes precedents on default urls such as static resources...
  //  notFound {
  //    val rq = request.pathInfo
  //    logger.error(s"Unsupported request '$rq', are you playing with me ?")
  //    html.notFound.render(ctx)
  //  }
  // ---------------------------------------------------------------------------------------------------------

  def slowcheck(num: Long, delay: String = "1s", againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
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

  get("/slowcheck/?") {
    slowcheck(nextInt, againUrl = Some("/slowcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def highcpucheck(num: Long, againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val engine = request.engine
    val pgen = new fr.janalyse.primes.PrimesGenerator[Long]
    val value = pgen.checkedValues.find(v => v.value == num)
    contentType = "text/html"
    val comment =
      if (withComment)
      Some(
        s"This page has a high CPU impact on the server, " +
          s"all values from 1 to $num are tested, because we " +
          s"need to give the prime or not prime rank of $num.")
      else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
  }

  get("/highcpucheck/:num") {
    val num = params("num").toLong
    highcpucheck(num)
  }

  get("/highcpucheck/?") {
    highcpucheck(nextInt, againUrl = Some("/slowcheck"))
  }
  // ---------------------------------------------------------------------------------------------------------

  def overcheck(num: Long, againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val engine = request.engine
    
    val value = engine.listAll().find(_.value == num)
    contentType = "text/html"
    val comment =
      if (withComment)
      Some(
        s"This page has a high heap impact on the server, " +
          s"all checked values are loaded from the database")
      else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
  }

  get("/overcheck/:num") {
    val num = params("num").toLong
    overcheck(num)
  }

  get("/overcheck/?") {
    overcheck(nextInt, againUrl = Some("/overcheck"))
  }
  // ---------------------------------------------------------------------------------------------------------

  
  def slowsqlcheck(num: Long, secs: Long = 1L, againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val engine = request.engine
    val dbpool = request.dbpool
    val value = engine.slowsqlcheck(num, dbpool, secs)
    val comment = if (withComment) Some(s"This page simulates a slow database with a minimum response time of $secs second(s).")else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
  }

  get("/slowsqlcheck/:num/:secs") {
    val secs = params.get("secs").map(_.toLong).getOrElse(1L)
    val num = params("num").toLong
    slowsqlcheck(num, secs)
  }
  get("/slowsqlcheck/:num") {
    val num = params("num").toLong
    slowsqlcheck(num)
  }

  get("/slowsqlcheck/?") {
    slowsqlcheck(nextInt, againUrl = Some("/slowsqlcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  var leak = List.empty[Array[Byte]]

  def leakedcheck(num: Long, howmany: String = "60Kb", againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val engine = request.engine
    leak = (Array.fill[Byte](howmany.toSize().toInt)(0x1)) :: leak
    val value = engine.check(num)
    val comment = if (withComment) Some(s"this page simulates a memory leak, you've just lost $howmany of heap memory.") else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
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

  get("/leakedcheck/?") {
    leakedcheck(nextInt, againUrl = Some("/leakedcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def sessionleakedcheck(num: Long, howmany: String = "320Kb", againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val engine = request.engine
    val newleak = (Array.fill[Byte](howmany.toSize().toInt)(0x1))
    val sessionleaks = Option(request.getSession.getAttribute("sessionmemleaks")).map(_.asInstanceOf[List[Array[Byte]]]) match {
      case None        => List(newleak)
      case Some(leaks) => newleak :: leaks
    }
    request.getSession.setAttribute("sessionmemleaks", sessionleaks)
    val value = engine.check(num)
    val comment = if (withComment) Some(s"this page simulates a session memory leak, you've just lost $howmany of heap memory in this current session.") else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
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

  get("/sessionleakedcheck/?") {
    sessionleakedcheck(nextInt, againUrl = Some("/sessionleakedcheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def jdbcleakcheck(num: Long, againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val engine = request.engine
    val value = engine.check(num)
    val dbpool = request.dbpool
    dbpool.map(ds => ds.getConnection) // oups one connection lost
    val comment = if (withComment) Some(s"this page simulates a jdbc connection leak, you've just lost one !") else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
  }

  get("/jdbcleakcheck/:num") {
    val num = params("num").toLong
    jdbcleakcheck(num)
  }

  get("/jdbcleakcheck/?") {
    jdbcleakcheck(nextInt, againUrl = Some("/jdbcleakcheck"))
  }
  // ---------------------------------------------------------------------------------------------------------

  def toomanylogscheck(num: Long, againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val started=System.currentTimeMillis()
    logger.debug("Check started for "+num+" by "+request.getSession.getId)  // So ByValue - logback java7
    logger.info("Check started for "+num)
    val engine = request.engine
    val value = engine.check(num)
    val duration=(System.currentTimeMillis()-started)
    logger.debug("Check finished for "+num+" in "+duration+" milliseconds"+" result is "+value.toString()+" by "+request.getSession.getId)
    logger.info("Check finished for "+num)
    logger.info("Checked "+value.toString()+" in "+duration+" milliseconds")
    val comment = if (withComment) Some(s"this page generates too many logs") else None
    logger.debug(s"page comment $comment")
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
  }

  get("/toomanylogscheck/:num") {
    logger.info("Checking"+params("num"))
    logger.debug("Checking "+params("num")+" by "+request.getSession.getId)
    val num = params("num").toLong
    logger.debug("Calling the check method with "+num+" value"+"by"+request.getSession.getId)
    toomanylogscheck(num)
  }

  get("/toomanylogscheck/?") {
    logger.info("Checking a random value")
    logger.debug("Checking a random value by "+request.getSession.getId)
    toomanylogscheck(nextInt, againUrl = Some("/toomanylogscheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def badlogscheck(num: Long, againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val started=System.currentTimeMillis()
    val engine = request.engine
    val value = engine.check(num)
    val duration=(System.currentTimeMillis()-started)
    logger.debug("Checked "+value.toString()+" in "+duration+" milliseconds") // So ByValue - logback java7
    logger.info("Checked "+value.toString()+" in "+duration+" milliseconds")  // So ByValue - logback java7
    val comment = if (withComment) Some(s"this page is using poorly written logs") else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl),comment)
  }

  get("/badlogscheck/:num") {
    val num = params("num").toLong
    badlogscheck(num)
  }

  get("/badlogscheck/?") {
    badlogscheck(nextInt, againUrl = Some("/badlogscheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  def goodlogscheck(num: Long, againUrl: Option[String] = None, withComment:Boolean=true) = forTestingPurposesOnly {
    logger.info("Checking#"+num)
    val started=System.currentTimeMillis()
    val engine = request.engine
    val value = engine.check(num)
    val duration=(System.currentTimeMillis()-started)
    lazy val message="Checked "+value.toString()+" in "+duration+" milliseconds"
    if (logger.isDebugEnabled()) logger.debug(message) //  // So ByValue - logback java7
    logger.info(message)  // So ByValue - logback java7
    val comment = if (withComment) Some(s"Good logging !") else None
    html.checkResult.render(ctx, num, value, gotoUrl(againUrl), comment)
  }

  get("/goodlogscheck/:num") {
    val num = params("num").toLong
    goodlogscheck(num)
  }

  get("/goodlogscheck/?") {
    goodlogscheck(nextInt, againUrl = Some("/goodlogscheck"))
  }

  // ---------------------------------------------------------------------------------------------------------

  val issues = Map(
    ("too much cpu", (n:Long, a: Option[String])=>highcpucheck(n,a,false)),
    ("too much heap", (n:Long, a: Option[String])=>overcheck(n,a,false)),
    ("server too slow", (n:Long, a: Option[String])=>slowcheck(n,"1s", a,false)),
    ("database too slow", (n:Long, a: Option[String])=>slowsqlcheck(n,2,a,false)),
    ("heap memory leak", (n:Long, a: Option[String])=>leakedcheck(n,"60kb",a,false)),
    ("session memory leak", (n:Long, a: Option[String])=>sessionleakedcheck(n,"500Kb",a,false)),
    ("jdbc connection leak", (n:Long, a: Option[String])=>jdbcleakcheck(n,a,false)),
    ("too many logs", (n:Long, a: Option[String])=>toomanylogscheck(n,a,false)),
    ("bad logs", (n:Long, a: Option[String])=>badlogscheck(n,a,false))
  )
  
  lazy val chosenIssue = {
    logger.info(s"${issues.size} issues type are available")
    val r = (math.random*8+1).toInt
    val (label, issue) = issues.toList(r)
    logger.info(s"Issue #$r has been chosen")
    issue
  }
  
  def issuecheck(num: Long, againUrl: Option[String] = None) = forTestingPurposesOnly {
    chosenIssue(num, againUrl)
  }

  get("/issuecheck/:num") {
    val num = params("num").toLong
    issuecheck(num)
  }

  get("/issuecheck/?") {
    issuecheck(nextInt, againUrl = Some("/issuecheck"))
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

  get("/prime/?") {
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

  get("/big/?") {
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


  // ---------------------------------------------------------------------------------------------------------
  // REMEMBER : ROUTE MATCHING IS BOTTOM UP !!!! 

  def factors(num: Long, againUrl: Option[String] = None) = {
    val engine = request.engine
    val factors = engine.factorize(num)
    html.factors.render(ctx, num, factors, gotoUrl(againUrl))
  }

  get("/factors/:num") {
    val num = params("num").toLong
    factors(num)
  }

  get("/factors/?") {
    factors(nextInt, Some("/factors"))
  }

  // ---------------------------------------------------------------------------------------------------------
  // REMEMBER : ROUTE MATCHING IS BOTTOM UP !!!! 

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
  get("/check/?") {
    check(nextInt, Some("/check"))
  }


  // ---------------------------------------------------------------------------------------------------------
  // REMEMBER : ROUTE MATCHING IS BOTTOM UP !!!! 

  get("/?") {
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
  
  
}
