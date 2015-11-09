package fr.janalyse.primesui

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import java.lang.management.ManagementFactory
import org.scalatest.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.squeryl.PrimitiveTypeMode._
import scala.concurrent._
import scala.concurrent.duration._


class SysInfoTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with PrimesDBInit {
  
  test("basic test") {
    val sys = (new SysInfo {}).sysinfoProps
    sys.size should be >(0)
    sys.toList.sorted.foreach{case (k,v) => info(s"$k = $v")}
  }
  
}