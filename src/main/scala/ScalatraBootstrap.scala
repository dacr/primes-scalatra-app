import fr.janalyse.primesui._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with PrimesDBInit with PrimesCacheInit {
  override def init(context: ServletContext) {
    dbSetup()
    context.mount(new PrimesServlet, "/*")
  }

  override def destroy(context: ServletContext) {
    dbTeardown()
  }

}
