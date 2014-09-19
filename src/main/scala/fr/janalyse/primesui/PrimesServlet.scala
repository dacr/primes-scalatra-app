package fr.janalyse.primesui

import org.scalatra._
import javax.sql.DataSource
import javax.naming.InitialContext
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.SessionFactory
import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter
import javax.servlet.ServletRequest


class PrimesServlet extends PrimesscalatraappStack {

  implicit class PrimesEngineRequest( request : ServletRequest ) {
    def engine : PrimesEngine = request.getServletContext().getAttribute( PrimesEngine.KEY ).asInstanceOf[PrimesEngine]
    def dbpool : Option[DataSource] = request.getServletContext().getAttribute( PrimesDBInit.KEY ).asInstanceOf[Option[DataSource]]
  }
  
  val rnd = scala.util.Random
  def nextInt = synchronized {rnd.nextInt(10000)}
  
  get("/") {
    val engine = request.engine
    <html>
      <body>
        <h1>Primes web application is ready.</h1>
    <p style="color:red"><b><i>classic webapp / mysql release of primes ui web application, classical design, almost all operations are synchronous.</i></b>
    </p>

        The database cache contains <b>{ engine.valuesCount }</b>
        already checked values, with <b>{ engine.primesCount }</b>
        primes found.
        The highest found prime is <b>{ engine.lastPrime.map(_.value).getOrElse(-1) }</b>.
        The application cache is <b>{if (engine.isUseCache) "enabled" else "disabled"}</b>.
        <h2>API</h2>
        <ul>
          <li><b>check/</b><i>$num</i> : to test if <i>$num</i> is a prime number or not.
                check a <a href={url("/check")}>random value</a>.
          </li>
          <li><b>prime/</b><i>$nth</i> : to get the nth prime, 1 -> 2, 2->3, 3->5, 4->7</li>
          <li><b>factors/</b><i>$num</i> : to get the prime factors of <i>$num</i>.
                Factorize a <a href={url("/factors")}>random value</a>.
          </li>
          <li><b>primes/</b><i>$below</i> : list primes lower than <i>$below</i>. List up to 
             <a href={url("/primes/1000")}>1K</a>,
             <a href={url("/primes/25000")}>25K</a>,
             <a href={url("/primes/50000")}>50K</a>,
             <a href={url("/primes/100000")}>100K</a>
          </li>
          <li><b>primes/</b><i>$below</i>/<i>$above</i> : list primes which are lower than <i>$below</i> and greater than <i>$above</i></li>
          <li><b>populate/</b><i>$upTo</i> : populate the database up to the specified value. Take care it calls a synchronized method.
            Populate up to 
             <a href={url("/populate/10000")}>10K</a>,
             <a href={url("/populate/25000")}>25K</a>,
             <a href={url("/populate/50000")}>50K</a>,
             <a href={url("/populate/100000")}>100K</a>,
             <a href={url("/populate/250000")}>250K</a>,
             <a href={url("/populate/500000")}>500K</a>
          </li>
          <li><b>ulam/</b><i>$size</i> : Dynamically draw an ulam spiral with the give <i>$size</i>. Take care of your CPUs and Heap ; this is a server side computation.
            Example for various size :
              <a href={url("/ulam/128")}>128</a>, 
              <a href={url("/ulam/256")}>256</a>, 
              <a href={url("/ulam/512")}>512</a>,
              <a href={url("/ulam/1024")}>1024</a> 
          </li>

        </ul>
        <h2>For testing purposes...</h2>
          <ul>
            <li><b>slowcheck/</b><i>$num</i>/<i>$secs</i> : to test if <i>$num</i> is a prime number or not, and wait <i>$secs</i> seconds at server side, this is for test purposes, default is 1 second.</li>
            <li><b>slowsql/</b><i>$num</i>/<i>$secs</i> : to test if <i>$num</i> is a prime number or not, and wait <i>$secs</i> seconds inside the database, this is for test purposes, default is 1 second, <b>cache feature is not used</b>.</li>
            <li><b>leakedcheck/</b><i>$num</i>/<i>$howmany</i> : to test if <i>$num</i> is a prime number or not, and leak <i>$howmany</i> megabytes at server side, this is for test purposes, default is 1Mb.</li>
            <li><b>big/</b><i></i>$howmanyKB : to test a response with an approximative size of <i>$howmany</i> kilobytes, default is 3Mb.</li>
          </ul>
        <h2>Admin</h2>
        <ul>
          <li><a href={url("/config")}>Application configuration</a></li>
        </ul>
      </body>
    </html>
  }

  get("/check/:num") {
    val engine = request.engine
    val num = params("num").toLong
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
      </body>
    </html>
  }
  
  get("/check") {
    val engine = request.engine
    val num = nextInt
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        <p>
          <i><a href={url("/check")}>Again</a></i> -
          <i><a href={url("/")}>Back to the menu</a></i>
        </p>
      </body>
    </html>
  }


  def slowcheck(num:Long, secs:Long=1L) = {
    val engine = request.engine
    Thread.sleep(secs * 1000L)
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulates a slow server with a minimum response time of { secs }
        seconds
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
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


  
  def slowsql(num:Long, secs:Long=1L) = {
    val engine = request.engine
    val dbpool = request.dbpool
    val value = engine.slowsqlcheck(num, dbpool, secs)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulates a slow database with a minimum response time of { secs }
        seconds
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
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
  
  def leakedcheck(num:Long, howmany:Int=1) = {
    val engine = request.engine
    leak=(Array.fill[Byte](1024 * 1024 * howmany)(0x1))::leak
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulates a memory leak, you've just lost { howmany } megabytes !
        seconds
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
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
  
  get("/prime/:nth") {
    val engine = request.engine
    val nth = params("nth").toLong
    val checked = engine.getPrime(nth).get // TODO : DANGEROUS
    import checked._
    <html>
      <body>
        <h1>{ value } is the { nth }th prime</h1>
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
      </body>
    </html>
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
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
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
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
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
          <i><a href={url("/")}>Back to the menu</a></i>
        </p>
      </body>
    </html>
  }
  
  
  get("/populate/:upto") {
    val engine = request.engine
    val upto = params("upto").toLong
    <html>
      <body>
        <h1>Primes generator state : {engine.populate(upto)}</h1>
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
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
    val engine = request.engine
    <html>
      <body>
        <h1>Configuration</h1>
        <form method="POST"
              enctype="application/x-www-form-urlencoded; charset=utf-8"
              action={url("/config")}>
          { 
    		if (engine.isUseCache)
	          <input type="checkbox" name="usecache" value="selected" checked="checked" >
                 Use application cache
              </input>
    		else
	          <input type="checkbox" name="usecache" value="selected">
                 Use application cache
              </input>
    		  
          }<br/>
          <input type="submit" value="Submit"/>
        </form>
      </body>
    </html>
  }
  
  post("/config") {
    val engine = request.engine
    params.get("usecache") match {
      case None => engine.setUseCache(false)
      case _ => engine.setUseCache(true)
    }
    redirect("/")
  }
  


  def big(howmanyKB:Int=3*1024) = {
    <html>
      <body>
         <h1>This is a big page, > 3Mb</h1>
{
  for { _ <- 1 to 16*howmanyKB} yield {
    <p>1234567891234567891234567890123456789012345678901234567</p>
  }
}
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
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

}
