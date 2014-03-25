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

  override def beforeAll() {
    dbSetup()
  }

  override def afterAll() {
    dbTeardown()
  }

  test("populate test") {
    val upTo=1000L
    PrimesEngine.populate(upTo)
    Await.result(PrimesEngine.worker.get, 60.seconds)
    
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
      val np = api.dbCheck(1001)
      np.map(_.isPrime).get should be (false)
    }
  }
  
}