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
        The highest found prime is <b>{ engine.lastPrime.map(_.value).getOrElse(-1) }</b>
        <h2>The API</h2>
        <ul>
          <li><b>check/</b><i>$num</i> : to test if <i>$num</i> is a prime number or not.
                check a <a href={url("/check")}>random value</a>.
          </li>
          <li><b>slowcheck/</b><i>$num</i>/<i>$secs</i> : to test if <i>$num</i> is a prime number or not, and wait <i>$secs</i> seconds at server side, this is for test purposes</li>
          <li><b>prime/</b><i>$nth</i> : to get the nth prime, 1 -> 2, 2->3, 3->5, 4->7</li>
          <li><b>factors/</b><i>$num</i> : to get the primer factors of <i>$num</i></li>
<!--
          <li><b>primes/</b><i>$to</i> : list primes up to <i>$to</i></li>
          <li><b>primes/</b><i>$form</i>/<i>$to</i> : list primes from <i>$from</i> to <i>$to</i></li>
-->
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
        <p><i><a href={url("/",includeContextPath=false)}>Back to the menu</a></i></p>
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
          <i><a href={url("/")}>Again</a></i> -
          <i><a href={url("/",includeContextPath=false)}>Back to the menu</a></i>
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
        this page simulate a slow server with a minimum response time of{ secs }
        seconds
        <p><i><a href={url("/",includeContextPath=false)}>Back to the menu</a></i></p>
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
        <p><i><a href={url("/",includeContextPath=false)}>Back to the menu</a></i></p>
      </body>
    </html>
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
        <p><i><a href={url("/",includeContextPath=false)}>Back to the menu</a></i></p>
      </body>
    </html>
  }
  
  get("/populate/:upto") {
    val engine = request.engine
    val upto = params("upto").toLong
    <html>
      <body>
        <h1>Primes generator state : {engine.populate(upto)}</h1>
        <p><i><a href={url("/",includeContextPath=false)}>Back to the menu</a></i></p>
      </body>
    </html>
  }

  
}
