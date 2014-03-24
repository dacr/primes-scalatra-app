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

class CachedValue(
  val value: Long,
  val isPrime: Boolean,
  val digitCount: Long,
  val nth: Long)

object PrimesDB extends Schema {
  val cachedValues = table[CachedValue]

  on(cachedValues)(cp => declare(
    cp.value is (unique, indexed),
    cp.isPrime is (indexed),
    cp.nth is (unique, indexed)))

}

trait PrimesDBApi {
  import PrimesDB._

  def dbAddValue(cachedValue:CachedValue) = {
    cachedValues.insert(cachedValue)
  }
  
  def dbValuesCount() = cachedValues.Count.single
  
  def dbPrimesCount() = 
    from(cachedValues) ( s =>
      where(s.isPrime===true)
      compute(count)
      ).single.measures
  
  def dbNotPrimesCount() = 
    from(cachedValues) ( s =>
      where(s.isPrime===false)
      compute(count)
      ).single.measures
  
  def dbLastPrime():Option[CachedValue] = 
    from(cachedValues) (cv=>
      where(cv.isPrime===true)
      select(cv)
      orderBy(cv.value desc)
      ).headOption
  
  def dbLastNotPrime():Option[CachedValue] = 
    from(cachedValues) (cv=>
      where(cv.isPrime===false)
      select(cv)
      orderBy(cv.value desc)
      ).headOption
  
  def dbCheck(value: Long): Option[CachedValue] =
    from(cachedValues) (cv =>
      where(cv.value === value)
      select(cv)
      ).headOption

  def dbGetPrime(nth:Long): Option[CachedValue] =
    from(cachedValues) (cv =>
      where(cv.nth === nth and cv.isPrime === true)
      select(cv)
      ).headOption
}


trait PrimesDBInit {
  val dbUsername = "optimus"
  val dbPassword = "bumblebee"
  val dbUrl = "jdbc:mysql://localhost/primes"

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
