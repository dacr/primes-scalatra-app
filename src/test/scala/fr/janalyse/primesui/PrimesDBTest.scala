package fr.janalyse.primesui

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import java.lang.management.ManagementFactory
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll

class PrimesDBTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with PrimesDBInit {

  override def beforeAll() {
    dbSetup()
  }

  override def afterAll() {
    dbTeardown()
  }

  test("Simple squirrel tests") {
    import org.squeryl.PrimitiveTypeMode._
    
    val api = new PrimesDBApi {}
    transaction {
      api.dbValuesCount should equal (0L)
      api.dbAddValue(new CachedValue(1L, false, 1L, 1L))
      val r = api.dbCheck(1L)
      r should be ('defined)
      r.get.isPrime should equal(false)
      api.dbValuesCount should equal (1L)
      api.dbLastNotPrime should be ('defined)
      api.dbLastNotPrime.get.isPrime should equal(false)
    }
  }
  
}