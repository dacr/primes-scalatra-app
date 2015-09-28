package fr.janalyse.primesui

import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html

class PrimesServletSpec extends MutableScalatraSpec {
  
  addServlet(classOf[PrimesServlet], "/*")

  "GET / on PrimesServlet" should {
    "return status 200" in {
      get("/") {
        status must_== 200
      }
    }
    
  }
}

