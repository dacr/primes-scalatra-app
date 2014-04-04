package fr.janalyse.primesui

import org.scalatra._
import scalate.ScalateSupport
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
  }
  
  val rnd = scala.util.Random
  def nextInt = synchronized {rnd.nextInt(10000)}
  
  get("/") {
    val engine = request.engine
    <html>
      <body>
        <h1>Primes web application is ready.</h1>
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
          <li><b>slowcheck/</b><i>$num</i>/<i>$secs</i> : to test if <i>$num</i> is a prime number or not, and wait <i>$secs</i> seconds at server side, this is for test purposes</li>
          <li><b>leakedcheck/</b><i>$num</i>/<i>$howmany</i> : to test if <i>$num</i> is a prime number or not, and leak <i>$howmany</i> megabytes at server side, this is for test purposes, default is 1Mb</li>
          <li><b>prime/</b><i>$nth</i> : to get the nth prime, 1 -> 2, 2->3, 3->5, 4->7</li>
          <li><b>factors/</b><i>$num</i> : to get the primer factors of <i>$num</i></li>
          <li><b>primes/</b><i>$below</i> : list primes lower than <i>$below</i></li>
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
<!--
          <li><b>ulam/</b><i>$size</i> : Dynamically draw an ulam spiral with the give <i>$size</i>. Take care of your CPUs and Heap ; this is a server side computation</li>
-->
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


  get("/slowcheck/:num/:secs") {
    val engine = request.engine
    val secs = params.get("secs").map(_.toLong).getOrElse(1L)
    Thread.sleep(secs * 1000L)
    val num = params("num").toLong
    val value = engine.check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulates a slow server with a minimum response time of{ secs }
        seconds
        <p><i><a href={url("/")}>Back to the menu</a></i></p>
      </body>
    </html>
  }
  
  
  var leak=List.empty[Array[Byte]]
  
  get("/leakedcheck/:num/:howmany") {
    val engine = request.engine
    val howmany = params.get("howmany").map(_.toInt).getOrElse(1)
    leak=(Array.fill[Byte](1024 * 1024 * howmany)(0x1))::leak
    val num = params("num").toLong
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
  
  get("/big") {
    <html>
      <body>
         <h1>This is a big page, > 3Mb</h1>
{
  for { _ <- 1 to 50000} yield {
    <p>123456789123456789123456789012345678901234567890123</p>
  }
}
      </body>
    </html>
  }
}
