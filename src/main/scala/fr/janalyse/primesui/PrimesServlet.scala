package fr.janalyse.primesui

import org.scalatra._
import org.scalatra.scalate._
import javax.sql.DataSource
import javax.naming.InitialContext
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.SessionFactory
import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter
import javax.servlet.ServletRequest


class PrimesServlet extends PrimesscalatraappStack with ScalateSupport {

  override def isDevelopmentMode = false
  
  
  /* wire up the precompiled templates */
  import org.fusesource.scalate.{ TemplateEngine, Binding }
  import org.fusesource.scalate.layout.DefaultLayoutStrategy
  override protected def defaultTemplatePath: List[String] = List("/WEB-INF/templates/views")
  override protected def createTemplateEngine(config: ConfigT) = {
    val engine = super.createTemplateEngine(config)
    engine.layoutStrategy = new DefaultLayoutStrategy(engine,
      TemplateEngine.templateTypes.map("/WEB-INF/templates/layouts/default." + _): _*)
    engine.packagePrefix = "templates"
    engine
  }
  /* end wiring up the precompiled templates */
  
  import javax.servlet.http.HttpServletRequest
  import collection.mutable
  override protected def templateAttributes(implicit request: HttpServletRequest): mutable.Map[String, Any] = {
    super.templateAttributes ++ mutable.Map.empty // Add extra attributes here, they need bindings in the build file
  }
  
  
  
  
  implicit class PrimesEngineRequest( request : ServletRequest ) {
    def engine : PrimesEngine = request.getServletContext().getAttribute( PrimesEngine.KEY ).asInstanceOf[PrimesEngine]
    def dbpool : Option[DataSource] = request.getServletContext().getAttribute( PrimesDBInit.KEY ).asInstanceOf[Option[DataSource]]
  }
  
  val rnd = scala.util.Random
  def nextInt = synchronized {rnd.nextInt(10000)}
  
  before("*") {
    response.setHeader("Cache-control", "no-cache, no-store, max-age=0, no-transform")
    
  }

  get("/") {
    val count = if (!request.engine.useSession) None else {
      val newcount = Option(request.getSession.getAttribute("count")).map(_.asInstanceOf[Long]) match {
        case None => 1L
        case Some(count) => count+1
      }
      request.getSession.setAttribute("count", newcount)
      Some(newcount)
    }

    contentType="text/html"
    scaml(
      "index",
      "engine"->request.engine,
      "checkUrl"->url("/check"),
      "factorsUrl"->url("/factors"),
      "primeUrl"->url("/prime"),
      "primesUrl"->url("/primes"),
      "populateUrl"->url("/populate"),
      "ulamUrl"->url("/ulam"),
      "slowcheckUrl"->url("/slowcheck"),
      "slowsqlUrl"->url("/slowsql"),
      "leakedcheckUrl"->url("/leakedcheck"),
      "bigUrl"->url("/big"),
      "aliveUrl"->url("/alive"),
      "sysinfoUrl"->url("/sysinfo"),
      "configUrl"->url("/config"),
      "count"->count,
      "version"->MetaInfo.version
    )
  }
  

  import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
  
//  def gotoMenu(implicit request: HttpServletRequest, response: HttpServletResponse) =
//     <a href={fullUrl("/", includeContextPath=true, includeServletPath=false)}>
//       Back to the menu
//     </a>

  
  def gotoUrl(goto:Option[String])(implicit request: HttpServletRequest, response: HttpServletResponse) =
    for {u <- goto } yield url(u, includeServletPath=false)
  
  def again(target:Option[String])(implicit request: HttpServletRequest, response: HttpServletResponse) =
    for { ref<- gotoUrl(target).toSeq} yield <i><a href={ref}>Again</a></i>
  
  
  def homeUrl(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    url("/.", includeServletPath=false)
  }
  def gotoMenu(implicit request: HttpServletRequest, response: HttpServletResponse) = {
     <i><a href={homeUrl}>Back to the menu</a></i>
  } 

/*  
  def check(num:Long, againUrl:Option[String]) = {
    val engine = request.engine
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        <p>{again(againUrl)} {gotoMenu}</p>
      </body>
    </html>
  }
*/
  def check(num:Long, againUrl:Option[String]) = {
    val engine = request.engine
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    contentType="text/html"
    scaml(
      "check",
      "num"->num,
      "value"->value,
      "againUrl"->gotoUrl(againUrl),
      "homeUrl"->homeUrl
      )
  }

  get("/check/:num") {
    val num = params("num").toLong
    check(num, None)
  }
  get("/check") {
    check(nextInt, Some("/check"))
  }
  
  
  private def forTestingOnly(proc: => xml.NodeSeq):xml.NodeSeq = {
    if (request.engine.useTesting) proc else {
      <html>
        <body>
	  <h1>Feature disabled...</h1>
          <p>{gotoMenu}</p>
	</body>
      </html>
    }
  }

  
  
  def slowcheck(num:Long, secs:Long=1L) = forTestingOnly {
    val engine = request.engine
    Thread.sleep(secs * 1000L)
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulates a slow server with a minimum response time of { secs }
        seconds
        <p>{gotoMenu}</p>
      </body>
    </html>
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
    slowcheck(nextInt)
  }


  
  def slowsql(num:Long, secs:Long=1L) = forTestingOnly {
    val engine = request.engine
    val dbpool = request.dbpool
    val value = engine.slowsqlcheck(num, dbpool, secs)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulates a slow database with a minimum response time of { secs }
        seconds
        <p>{gotoMenu}</p>
      </body>
    </html>
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
    slowsql(nextInt)
  }

  
  
  var leak=List.empty[Array[Byte]]
  
  def leakedcheck(num:Long, howmany:Int=1) = forTestingOnly {
    val engine = request.engine
    leak=(Array.fill[Byte](1024 * 1024 * howmany)(0x1))::leak
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulates a memory leak, you've just lost { howmany } megabytes !
        seconds
        <p>{gotoMenu}</p>
      </body>
    </html>
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
    leakedcheck(nextInt)
  }
  
  
  def prime(nth:Long, againUrl:Option[String]) = {
    val engine = request.engine
    val checked = engine.getPrime(nth).get // TODO : DANGEROUS
    <html>
      <body>
        <h1>{ checked.value } is the { checked.nth }th prime</h1>
        <p>{again(againUrl)} {gotoMenu}</p>
      </body>
    </html>    
  }
  
  get("/prime/:nth") {
    val nth = params("nth").toLong
    prime(nth, None)
  }
  
  get("/prime") {
    prime(nextInt, Some("/prime"))
  }
  
  
  def primes(below:Long, above:Long=0L) = {
    val engine = request.engine
    val primes = engine.listPrimes(below, above)
    <html>
      <body>
         <h1>Primes number above {above} and below {below}</h1>
         <ul>
         {
		   for { prime <- primes} yield {
		     <li><pre>{prime.nth} --> {prime.value}</pre></li>
		   }
         }
         </ul>
        <p>{gotoMenu}</p>
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
        <p>{gotoMenu}</p>
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
          <i><a href={url("/factors")}>Again</a></i> -
          {gotoMenu}
        </p>
      </body>
    </html>
  }
  

  get("/populate/:upto") {
    val uptoAsked = params("upto").toLong
    val limit = 2000000L
    val (upto,msg) =
      if (request.engine.useTesting) 
        uptoAsked->None
      else 
        math.min(limit, uptoAsked)->Some(s"$uptoAsked asked, authorized maximum is $limit !")
    val engine = request.engine
    
    <html>
      <body>
        <h1>Primes generator state : { engine.populate(upto) }</h1>
        { msg.map { m => <p>WARN : { m }</p> }.getOrElse(xml.NodeSeq.Empty) }
        <p>{ gotoMenu }</p>
      </body>
    </html>

  }

  get("/ulam/:sz") {
    val engine = request.engine
    val sz = params.get("sz").map(_.toInt).getOrElse(100)
    val bytes = engine.ulamAsPNG(sz)
    contentType = "image/png"
    response.getOutputStream().write(bytes)
  }

  get("/config") {
    forTestingOnly {
      val engine = request.engine
      <html>
        <body>
          <h1>Configuration</h1>
          <form method="POST" enctype="application/x-www-form-urlencoded; charset=utf-8" action={ url("/config") }>
            {
              if (engine.isUseCache)
                <input type="checkbox" name="usecache" value="selected" checked="checked">
                  Use application cache
                </input>
              else
                <input type="checkbox" name="usecache" value="selected">
                  Use application cache
                </input>

            }<br/>
            <input type="submit" value="Submit"/>
          </form>
        <p>{gotoMenu}</p>
        </body>
      </html>
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
  


  def big(howmanyKB:Int=3*1024) = forTestingOnly {
    <html>
      <body>
         <h1>This is a big page, > 3Mb</h1>
{
  for { _ <- 1 to 16*howmanyKB} yield {
    <p>1234567891234567891234567890123456789012345678901234567</p>
  }
}
        <p>{gotoMenu}</p>
      </body>
    </html>
  }


  get("/big/:howmany") {
    val howmanyKB = params.get("howmany").map(_.toInt).getOrElse(3*1024)
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
        "^java[.]runtime[.].*"
        ).map(_.r)
    val props = System.getProperties.toMap
    props.filter{case (k,v) =>
      res.exists(_.findFirstIn(k).isDefined)
      }
  }
  
  val extendedProps = {
    import java.lang.management.ManagementFactory
    val os = ManagementFactory.getOperatingSystemMXBean()
    val rt = ManagementFactory.getRuntimeMXBean()
    Map(
        "extra.availableProcessors"->java.lang.Runtime.getRuntime.availableProcessors.toString(),
        "extra.sysinfo"->"enabled"
        )
  }
  
  get("/sysinfo") {
    response.setContentType("text/plain")
    if (request.engine.useTesting) {
      (selectedProps++extendedProps).toList.sorted.map{case (k,v)=> k+"="+v}.mkString("\n")
    } else {
      "extra.sysinfo=disabled"
    }
  }

}
