package fr.janalyse.primesui

import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html

class PrimesServletSpec extends MutableScalatraSpec {

  val he = addServlet(classOf[PrimesServlet], "/*")

  val pe = new PrimesEngine().setup()
  val db = new PrimesDBInit{dbSetup()}
  
  val ctx = he.getServlet.getServletConfig.getServletContext
  ctx.setAttribute(PrimesEngine.KEY, pe)
  ctx.setAttribute(PrimesDBInit.KEY, db.dbpool)
  
  
  "GET / on PrimesServlet" should {
    "return status 200" in {
      get("/") {
        Thread.sleep(300 * 1000L)
        status must_== 200
      }
    }

  }
}

