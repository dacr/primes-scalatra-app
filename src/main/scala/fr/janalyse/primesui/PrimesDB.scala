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
import fr.janalyse.primes.CheckedValue

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

  def dbAddValue(cachedValue: CachedValue) = {
    cachedValues.insert(cachedValue)
  }

  def dbValuesCount() = cachedValues.Count.single

  def dbPrimesCount() =
    from(cachedValues)(s =>
      where(s.isPrime === true)
        compute (count)
    ).single.measures

  def dbNotPrimesCount() =
    from(cachedValues)(s =>
      where(s.isPrime === false)
        compute (count)
    ).single.measures

  def dbLastPrime(): Option[CachedValue] =
    from(cachedValues)(cv =>
      where(cv.isPrime === true)
        select (cv)
        orderBy (cv.value desc)
    ).headOption

  def dbLastNotPrime(): Option[CachedValue] =
    from(cachedValues)(cv =>
      where(cv.isPrime === false)
        select (cv)
        orderBy (cv.value desc)
    ).headOption

  def dbCheck(value: Long): Option[CachedValue] =
    from(cachedValues)(cv =>
      where(cv.value === value)
        select (cv)
    ).headOption

  def dbGetPrime(nth: Long): Option[CachedValue] =
    from(cachedValues)(cv =>
      where(cv.nth === nth and cv.isPrime === true)
        select (cv)
    ).headOption

  def dbListPrimes(below: Long, above: Long = 0L) = {
    val qry = from(cachedValues)(cv =>
      where(cv.isPrime === true and (cv.value gte above) and (cv.value lte below))
        select (cv)
        orderBy (cv.value asc)
    )
    qry.toIterator
  }

  def dbList(below: Long, above: Long = 0L) = {
    val qry = from(cachedValues)(cv =>
      where((cv.value gte above) and (cv.value lte below))
        select (cv)
        orderBy (cv.value asc)
    )
    qry.toIterator
  }

}

object PrimesDBInit {
  val KEY = "DBPOOL"
}

trait PrimesDBInit {
  import util.Properties._

  val dsName = "primes-ds"

  def classicPoolBuild():ComboPooledDataSource = {
    val dbHost = None
      .orElse(propOrNone("PRIMES_DB_HOST"))
      .orElse(envOrNone("PRIMES_DB_HOST"))
      .orElse(envOrNone("OPENSHIFT_MYSQL_DB_HOST"))
      .orElse(propOrNone("RDS_HOSTNAME")) // AWS
      .getOrElse("localhost")

    val dbPort = None
      .orElse(propOrNone("PRIMES_DB_PORT"))
      .orElse(envOrNone("PRIMES_DB_PORT"))
      .orElse(envOrNone("OPENSHIFT_MYSQL_DB_PORT"))
      .orElse(propOrNone("RDS_PORT")) // AWS
      .getOrElse("3306").toInt

    val dbUsername = None
      .orElse(envOrNone("PRIMES_DB_USERNAME"))
      .orElse(propOrNone("PRIMES_DB_USERNAME"))
      .orElse(propOrNone("RDS_USERNAME")) // AWS
      .getOrElse("optimus")

    val dbPassword = None
      .orElse(envOrNone("PRIMES_DB_PASSWORD"))
      .orElse(propOrNone("PRIMES_DB_PASSWORD"))
      .orElse(propOrNone("RDS_PASSWORD")) // AWS
      .getOrElse("bumblebee")

    val dbName = None
      .orElse(envOrNone("PRIMES_DB_NAME"))
      .orElse(propOrNone("PRIMES_DB_NAME"))
      .orElse(propOrNone("RDS_DB_NAME")) // AWS
      .getOrElse("primes")

    val dbUrl = s"jdbc:mysql://$dbHost:$dbPort/$dbName"

    val cpds = new ComboPooledDataSource(dsName)
    cpds.setDriverClass("com.mysql.jdbc.Driver")
    cpds.setJdbcUrl(dbUrl)
    cpds.setUser(dbUsername)
    cpds.setPassword(dbPassword)
    cpds
  }

  def viaUrlPoolBuild():Option[ComboPooledDataSource] = {
    for { dbUrl <- propOrNone("JDBC_CONNECTION_STRING") } yield {
      val cpds = new ComboPooledDataSource(dsName)
      cpds.setDriverClass("com.mysql.jdbc.Driver")
      cpds.setJdbcUrl(dbUrl)
      cpds
      }
  }


  //private var pool: Option[ComboPooledDataSource] = None
  var dbpool: Option[ComboPooledDataSource] = None

  protected def dbSetup() = {
    val cpds = viaUrlPoolBuild() getOrElse classicPoolBuild()
    cpds.setMaxPoolSize(20)
    cpds.setMinPoolSize(0)
    cpds.setInitialPoolSize(0)
    cpds.setMaxIdleTime(30)

    def connection = Session.create(cpds.getConnection, new MySQLAdapter)
    SessionFactory.concreteFactory = Some(() => connection)
    try {
      transaction {
        PrimesDB.create
      }
    } catch {
      case e:Exception => // Probably already created - TODO enhanhcements required
    }
    dbpool = Some(cpds)
  }

  protected def dbTeardown() {
    dbpool.foreach(_.close)
    dbpool = None
  }

}
