package fr.janalyse.primesui

import org.scalatra._
import scalate.ScalateSupport
import javax.sql.DataSource
import javax.naming.InitialContext
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.SessionFactory
import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter

sealed trait DataSourceProvider {
  def dataSource: DataSource
  
  SessionFactory.concreteFactory = Some(() => connection)
  def connection = Session.create(dataSource.getConnection, new MySQLAdapter)
}

trait TestDataSourceProvider extends DataSourceProvider {
  lazy val dataSource: DataSource = {
    var cpds = new ComboPooledDataSource
    cpds.setDriverClass("com.mysql.jdbc.Driver")
    cpds.setJdbcUrl("jdbc:mysql://localhost/primes")
    cpds.setUser("tomcat")
    cpds.setPassword("tomcat")
    cpds
  }
  
}
trait JndiDataSourceProvider extends DataSourceProvider {
  lazy val dataSource = new InitialContext().lookup("java:comp/env/jdbc/primes").asInstanceOf[DataSource]
}


class PrimesServlet extends PrimesscalatraappStack {

  import PrimesEngine._

  get("/") {
    <html>
      <body>
        <h1>Prime web application is ready.</h1>
        The database cache contains{ valuesCount }
        already checked values, with{ primesCount }
        primes found.
        The highest found prime is{ lastPrime.map(_.value).getOrElse(-1) }
        <h2>The API</h2>
        <ul>
          <li><b>check/</b><i>$num</i> : to test if <i>$num</i> is a prime number or not</li>
          <li><b>slowcheck/</b><i>$num</i>/<i>$secs</i> : to test if <i>$num</i> is a prime number or not, and wait <i>$secs</i> seconds at server side, this is for test purposes</li>
          <li><b>prime/</b><i>$nth</i> : to get the nth prime, 1 -> 2, 2->3, 3->5, 4->7</li>
          <li><b>factors/</b><i>$num</i> : to get the primer factors of <i>$num</i></li>
          <!--
          <li><b>primes/</b><i>$to</i> : list primes up to <i>$to</i></li>
          <li><b>primes/</b><i>$form</i>/<i>$to</i> : list primes from <i>$from</i> to <i>$to</i></li>
          <li><b>populate/</b><i>$upTo</i> : populate the database up to the specified value. Take care it calls a synchronized method.</li>
          <li><b>ulam/</b><i>$size</i> : Dynamically draw an ulam spiral with the give <i>$size</i>. Take care of your CPUs and Heap ; this is a server side computation</li>
-->
        </ul>
      </body>
    </html>
  }

  get("/check/:num") {
    val num = params("num").toLong
    val value = check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
      </body>
    </html>
  }

  get("/slowcheck/:num/:secs") {
    val secs = params.get("secs").map(_.toLong).getOrElse(1L)
    Thread.sleep(secs * 1000L)
    val num = params("num").toLong
    val value = check(num)
    val isPrime = value.map(_.isPrime).getOrElse(false)
    <html>
      <body>
        <h1>{ num } is the { value.map(_.nth).getOrElse(-1) }th { if (isPrime) "" else "not" } prime</h1>
        this page simulate a slow server with a minimum response time of{ secs }
        seconds
      </body>
    </html>
  }

  get("/prime/:nth") {
    val nth = params("nth").toLong
    val checked = getPrime(nth).get // TODO : DANGEROUS
    import checked._
    <html>
      <body>
        <h1>{ value } is the { nth }th prime</h1>
      </body>
    </html>
  }

  get("/factors/:num") {
    val num = params("num").toLong
    val factors = factorize(num).get // TODO : DANGEROUS 
    <html>
      <body>
        {
          if (factors.isEmpty) <h1>{ num } = { num } <i>and is prime</i> </h1>
          else <h1>{ num } = { factors.mkString(" * ") }</h1>
        }
      </body>
    </html>
  }
}
