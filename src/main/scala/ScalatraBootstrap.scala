import fr.janalyse.primesui._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with PrimesDBInit {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)
  
  override def init(context: ServletContext) {
    logger.info("primesui is starting")
    dbSetup()
    
    System.setProperty("scalate.mode",         "production")
    System.setProperty("scalate.allowReload",  "false")
    System.setProperty("scalate.allowCaching", "false")
    
    val pe = new PrimesEngine()
    pe.setup()
    
    context.mount(new PrimesServlet, "/*")

    context.setAttribute(PrimesEngine.KEY, pe)
    context.setAttribute(PrimesDBInit.KEY, dbpool)

    logger.info("primesui started")
  }

  override def destroy(context: ServletContext) {
    logger.info("primesui is stopping")
    val pe = context.getAttribute(PrimesEngine.KEY).asInstanceOf[PrimesEngine]
    pe.teardown()
    dbTeardown()
    logger.info("primesui stopped")
  }

}
