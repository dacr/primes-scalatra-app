import fr.janalyse.primesui._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with PrimesDBInit {

  override def init(context: ServletContext) {
    dbSetup()
    context.mount(new PrimesServlet, "/*")
    val pe = new PrimesEngine()
    pe.setup()
    context.setAttribute(PrimesEngine.KEY, pe)
  }

  override def destroy(context: ServletContext) {
    val pe = context.getAttribute(PrimesEngine.KEY).asInstanceOf[PrimesEngine]
    pe.teardown()
    dbTeardown()
  }

}
