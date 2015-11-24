package fr.janalyse.primesui

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.annotations.Column
import java.util.Date
import java.sql.Timestamp
import javax.sql.DataSource
import org.squeryl.Session
import org.squeryl.SessionFactory
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.adapters.MySQLAdapter
import fr.janalyse.primes.CheckedValue
import collection.JavaConversions._

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
    cp.nth is (indexed)))

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

//  def dbLastPrime(): Option[CachedValue] =
//    from(cachedValues)(cv =>
//      where(cv.isPrime === true)
//        select (cv)
//        orderBy (cv.value desc)
//    ).headOption

  def dbLastPrime(): Option[CachedValue] = {
    val highest = from(cachedValues)(cv =>
      where(cv.isPrime === true)
        compute (max(cv.value))
    ).single.measures
    highest.flatMap(dbCheck)
  }

    
//  def dbLastNotPrime(): Option[CachedValue] =
//    from(cachedValues)(cv =>
//      where(cv.isPrime === false)
//        select (cv)
//        orderBy (cv.value desc)
//    ).headOption

  def dbLastNotPrime(): Option[CachedValue] = {
    val highest = from(cachedValues)(cv =>
      where(cv.isPrime === false)
        compute(max(cv.value))
    ).single.measures
    highest.flatMap(dbCheck)
  }

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

  def dbListAll() = {
    val qry = from(cachedValues)(cv =>
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
  private val logger = org.slf4j.LoggerFactory.getLogger("fr.janalyse.primesui.PrimesDBInit")

  def env = System.getenv.toMap
  def renvOrNone(re:String):Option[String] = env.collect {case (k,v) if re.r.findFirstIn(k).isDefined => v}.headOption
  def propOrEnvOrNone(key:String):Option[String] = propOrNone(key).orElse(envOrNone(key))


  private def internalClassicPoolBuild():ComboPooledDataSource = {
    val dbHost = None
      .orElse(propOrEnvOrNone("PRIMES_DB_HOST"))
      .orElse(propOrEnvOrNone("OPENSHIFT_MYSQL_DB_HOST"))             // OPENSHIFT
      .orElse(propOrEnvOrNone("RDS_HOSTNAME"))                        // AMAZON AWS
      .orElse(renvOrNone("""DOCKER_PRIMES_DB_PORT_\d+_TCP_ADDR"""))   // DOCKER
      .getOrElse("localhost")

    val dbPort = None
      .orElse(propOrEnvOrNone("PRIMES_DB_PORT"))
      .orElse(propOrEnvOrNone("OPENSHIFT_MYSQL_DB_PORT"))             // OPENSHIFT
      .orElse(propOrEnvOrNone("RDS_PORT"))                            // AMAZON AWS
      .orElse(renvOrNone("""DOCKER_PRIMES_DB_PORT_\d+_TCP_PORT"""))   // DOCKER
      .getOrElse("3306").toInt

    val dbUsername = None
      .orElse(propOrEnvOrNone("PRIMES_DB_USERNAME"))
      .orElse(propOrEnvOrNone("RDS_USERNAME"))                        // AMAZON AWS
      .getOrElse("optimus")

    val dbPassword = None
      .orElse(propOrEnvOrNone("PRIMES_DB_PASSWORD"))
      .orElse(propOrEnvOrNone("RDS_PASSWORD"))                        // AMAZON AWS
      .getOrElse("bumblebee")

    val dbName = None
      .orElse(propOrEnvOrNone("PRIMES_DB_NAME"))
      .orElse(propOrEnvOrNone("RDS_DB_NAME"))                         // AMAZON AWS
      .getOrElse("primes")

    val dbUrl = s"jdbc:mysql://$dbHost:$dbPort/$dbName?user=$dbUsername&password=$dbPassword"
    //val dbUrl = s"jdbc:mysql://$dbHost:$dbPort/$dbName"

    makeInternalDataSource(dbUrl)
  }

  
  private def internalViaUrlPoolBuild():Option[ComboPooledDataSource] = {
    for {
      dbUrl <- propOrEnvOrNone("JDBC_CONNECTION_STRING").filter(_.trim.size >0)
      } yield makeInternalDataSource(dbUrl)
  }
  
  private def makeInternalDataSource(url:String):ComboPooledDataSource = {
      val dsName = "primes-ds"
      val driver = "com.mysql.jdbc.Driver"
      logger.info("Using built in internal datasource, C3P0 based")
      Class.forName(driver).newInstance()
      val cpds = new ComboPooledDataSource(dsName)
      cpds.setDriverClass(driver)
      cpds.setJdbcUrl(url)
      cpds.setMaxPoolSize(100)
      cpds.setMinPoolSize(2)
      cpds.setInitialPoolSize(2)
      cpds.setMaxIdleTime(30)
      cpds
  }

  private def externalPool(): Option[DataSource] = {
    import javax.naming.{ InitialContext, Context }
    import scala.util.{ Try, Success }
    val name = "java:/comp/env/jdbc/primesui"
    val initContext = new InitialContext()
    Try { initContext.lookup(name) } match {
      case Success(ds: DataSource) => 
        logger.info(s"Found an external datasource named '$name' ")
        Some(ds)
      case _ => 
        None
    }
  }
  

  // small hack mandatory in order to close the internal pool when the application is undeployed
  var dbpool:Option[DataSource] = None
  var dbpoolShutdDownHook: Option[{def close():Unit}]=None

  protected def dbSetup() = {
    logger.info("Application database setup")
    val cpds:DataSource = {
      externalPool() getOrElse {
        val internal = internalViaUrlPoolBuild() getOrElse internalClassicPoolBuild()
        dbpoolShutdDownHook = Some(internal)
        internal
      }
    }
    logger.info("Selected datasource classname : "+cpds.getClass.getName)

    def connection = Session.create(cpds.getConnection, new MySQLAdapter)
    SessionFactory.concreteFactory = Some(() => connection)
    try {
      transaction {
        PrimesDB.create
        logger.info("Database created")
      }
    } catch {
      case e:Exception => // Probably already created - TODO enhancements required
        logger.warn(s"Couldn't created the database (${e.getClass.getName} - ${e.getMessage})")
    }
    dbpool = Some(cpds)
  }

  protected def dbTeardown() {
    logger.info("Application database cleanup")
    dbpoolShutdDownHook.foreach(_.close)
    dbpool=None
  }

}
