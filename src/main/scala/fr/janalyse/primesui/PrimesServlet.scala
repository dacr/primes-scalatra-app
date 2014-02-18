package fr.janalyse.primesui

import org.scalatra._
import scalate.ScalateSupport

class PrimesServlet extends PrimesscalatraappStack {

  import PrimesEngine._
  
  get("/") {
    <html>
      <body>
        <h1>Prime web application is ready.</h1>
        The database cache contains {valuesCount} already checked values, with {primesCount} primes found.
        The highest found prime is {lastPrime.map(_.value).getOrElse(-1)}
        <h2>The API</h2>
        <ul>
          <li>check/<i>$num</i> : to test if <i>$num</i> is a prime number or not</li>
<!--
          <li>slowcheck/<i>$num</i>/<i>$secs</i> : to test if <i>$num</i> is a prime number or not, and wait <i>$secs</i> seconds at server side, this is for test purposes</li>
          <li>prime/<i>$nth</i> : to get the nth prime, 1 -> 2, 2->3, 3->5, 4->7</li>
          <li>factors/<i>$num</i> : to get the primer factors of <i>$num</i></li>
          <li>primes/<i>$to</i> : list primes up to <i>$to</i></li>
          <li>primes/<i>$form</i>/<i>$to</i> : list primes from <i>$from</i> to <i>$to</i></li>
          <li>populate/<i>$upTo</i> : populate the database up to the specified value. Take care it calls a synchronized method.</li>
          <li>ulam/<i>$size</i> : Dynamically draw an ulam spiral with the give <i>$size</i>. Take care of your CPUs and Heap ; this is a server side computation</li>
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
        <h1>{num} is the {value.map(_.nth).getOrElse(-1)}th {if(isPrime) "" else "not" } prime</h1>
      </body>
    </html>
  }

}
