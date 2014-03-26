package fr.janalyse.primesui

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import java.lang.management.ManagementFactory
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.squeryl.PrimitiveTypeMode._
import scala.concurrent._
import scala.concurrent.duration._


class PrimesDBTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with PrimesDBInit {

  val engine = new PrimesEngine()

  override def beforeAll() {
    dbSetup()
    engine.setup()
    engine.setUseCache(false)
  }

  override def afterAll() {
    dbTeardown()
    engine.teardown()
  }
  
  val upTo=1000L

  test("populate test") {
    engine.populate(upTo)
    Await.result(engine.worker.get, 60.seconds)
  }
  
  test("db primes api tests") {
    val api = new PrimesDBApi {}
    transaction {
      api.dbValuesCount should be >=(upTo - 1)
      api.dbLastNotPrime should be ('defined)
      api.dbLastPrime should be ('defined)
      api.dbLastPrime.map(_.value).get should be >=(997L)
      api.dbLastNotPrime.map(_.value).get should be >=(1000L)
    }
    
    transaction {
      val p  = api.dbCheck(997)
      p.map(_.isPrime).get should be (true)
      val np = api.dbCheck(1000)
      np.map(_.isPrime).get should be (false)
    }
  }
  
}