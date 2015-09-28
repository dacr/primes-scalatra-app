package fr.janalyse.primesui

import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html

class PrimesServletSpec extends MutableScalatraSpec {

  val he = addServlet(classOf[PrimesServlet], "/*")
  
  override lazy val servletContextHandler = {
    import org.eclipse.jetty.servlet._
    val handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.setContextPath(contextPath)
    handler.addEventListener(new org.scalatra.servlet.ScalatraListener)
    handler.setResourceBase(resourceBasePath)
    handler
  }
  
  
  "GET / on PrimesServlet" should {
    "return status 200" in {
      get("/") {
        status must_== 200
      }
    }
  }
  
  
  "GET /check/123 on PrimesServlet" should {
    "return status 200" in {
      get("/check/123") {
        status must_== 200
        body must contain("not prime")
      }
    }
  }

  
  "GET /check/17 on PrimesServlet" should {
    "return status 200" in {
      get("/check/17") {
        status must_== 200
        body must not contain("not prime")
      }
    }
  }

  
}

