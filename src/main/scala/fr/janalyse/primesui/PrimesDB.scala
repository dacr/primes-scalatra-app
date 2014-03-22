package fr.janalyse.primesui

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.annotations.Column
import java.util.Date
import java.sql.Timestamp
import org.squeryl.Session
import org.squeryl.SessionFactory
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.adapters.MySQLAdapter

class CachedPrime(
  val value: Long,
  val isPrime: Boolean,
  val digitCount: Long,
  val nth: Long)

object PrimesDB extends Schema {
  val cachedPrimes = table[CachedPrime]

  on(cachedPrimes)(cp => declare(
    cp.value is (unique, indexed),
    cp.isPrime is (indexed),
    cp.nth is (unique, indexed)))

}

trait PrimesDBApi {
  import PrimesDB._
  
  def getPrime(value: Long): Option[CachedPrime] =
    cachedPrimes.find(s => s.value == value)

}

trait PrimesDBInit {

  val dbUsername = "optimus"
  val dbPassword = "bumblebee"
  val dbUrl = "jdbc:h2:mem:squeryltryout"

  private var cpdsopt: Option[ComboPooledDataSource] = None

  protected def dbSetup() = {
    val cpds = new ComboPooledDataSource
    cpds.setDriverClass("com.mysql.jdbc.Driver")
    cpds.setJdbcUrl(dbUrl)
    cpds.setUser(dbUsername)
    cpds.setPassword(dbPassword)
    SessionFactory.concreteFactory = Some(() => connection)
    def connection = Session.create(cpds.getConnection, new MySQLAdapter)
    cpdsopt = Some(cpds)
  }

  protected def dbTeardown() {
    cpdsopt.foreach(_.close)
    cpdsopt = None
  }


}
