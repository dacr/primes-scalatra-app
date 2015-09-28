package fr.janalyse.primesui

import fr.janalyse.primes._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import org.squeryl.PrimitiveTypeMode._
import net.sf.ehcache.CacheManager
import java.lang.management.ManagementFactory
import net.sf.ehcache.management.ManagementService
import net.sf.ehcache.Element
import net.sf.ehcache.Cache
import fr.janalyse.jmx._
import org.slf4j.LoggerFactory

trait PrimesEngineMBean {
  def isUseCache(): Boolean
  def setUseCache(v: Boolean)
}

object PrimesEngine {
  val KEY = "primes-engine"
}

class PrimesEngine extends PrimesDBApi with PrimesEngineMBean {
  private val logger = LoggerFactory.getLogger(getClass)

  private val oname = s"primes:name=PrimesEngine"

  def setup() = {
    logger.info("PrimesEngine is starting")
    JMX.register(this, oname)
    CacheManager.create()
    val manager = CacheManager.newInstance();
    val mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
    logger.info("PrimesEngine started")
    this
  }

  def teardown() {
    logger.info("PrimesEngine is stopping")
    JMX.unregister(oname)
    CacheManager.getInstance().shutdown()
    logger.info("PrimesEngine stopped")
  }

  def toBoolean(in: String): Boolean = {
    in.toLowerCase().trim match {
      case "true" | "1" | "on" | "yes" | "enabled" | "up" => true
      case _ => false
    }
  }

  private def provOrEnvOrDefault(key: String, default: Boolean): Boolean = {
    import scala.util.Properties._
    propOrNone(key)
      .orElse(envOrNone(key))
      .map(toBoolean)
      .getOrElse(default)
  }

  val useTesting = provOrEnvOrDefault("PRIMESUI_TESTING", true)
  val useSession = provOrEnvOrDefault("PRIMESUI_SESSION", true)
  private var useCaches = provOrEnvOrDefault("PRIMESUI_CACHE", false)

  def isUseCache(): Boolean = useCaches
  def setUseCache(v: Boolean) { useCaches = v }

  private lazy val cache = CacheManager.getInstance()
  private lazy val cachedValuesCache = cache.getCache("CachedValuesCache")
  private lazy val countersCache = cache.getCache("CountersCache")
  private lazy val primesCache = cache.getCache("PrimesCache")
  private lazy val lastCache = cache.getCache("LastCache")
  private lazy val factorsCache = cache.getCache("FactorsCache")
  private lazy val ulamCache = cache.getCache("UlamCache")

  private def populatePrimesIfRequired(upTo: Long = 100000, grouped: Int = 1000) = {
    logger.info("populate primes if required")
    val pgen = new PrimesGenerator[Long]
    val fall = Future {
      val started = System.currentTimeMillis()
      val logger = LoggerFactory.getLogger("fr.janalyse.primesui.PrimesEngine.PopulateFuture")
      logger.info(s"Asynchronous primes populate process started - SQL insertion are grouped by $grouped")
      var resumedStream = transaction {
        pgen.checkedValues(dbLastPrime.map(conv), dbLastNotPrime.map(conv))
      }
      var counter:Long=0L
      while (resumedStream.head.value <= upTo) {
        transaction {
          (1 to grouped) foreach { _ =>
            dbAddValue(conv(resumedStream.head))
            resumedStream = resumedStream.tail
          }
        }
        counter+=grouped
      }
      val secs=(System.currentTimeMillis()-started)/1000
      logger.info(s"Done in $secs seconds - $counter values inserted into the database")
      'done
    }
    fall.onFailure {
      case x => logger.error(s"Something wrong happens during values insertion into the database", x)
    }
    fall
  }

  def conv(nv: CheckedValue[Long]): CachedValue = {
    new CachedValue(nv.value, nv.isPrime, nv.digitCount, nv.nth)
  }
  def conv(cv: CachedValue): CheckedValue[Long] = {
    new CheckedValue[Long](cv.value, cv.isPrime, cv.digitCount, cv.nth)
  }

  var worker: Option[Future[Symbol]] = None

  def populate(upTo: Long) = this.synchronized {
    if (worker.isEmpty || worker.get.isCompleted) {
      val populateFuture = populatePrimesIfRequired(upTo)
      worker = Some(populateFuture)
      'Started
    } else {
      'StillInProgress
    }
  }

  def usingcache[T](mkresult: => T, key: Any, cache: Cache): T = {
    if (!useCaches) mkresult
    else {
      val item = cache.get(key)
      if (item == null) {
        val cv = mkresult
        cache.put(new Element(key, cv))
        cv
      } else item.getObjectValue().asInstanceOf[T]
    }
  }

  def valuesCount(): Long = {
    usingcache(transaction { dbValuesCount() }, "values", countersCache)
  }

  def primesCount(): Long = {
    usingcache(transaction { dbPrimesCount() }, "primes", countersCache)
  }

  def notPrimesCount(): Long = {
    usingcache(transaction { dbNotPrimesCount() }, "notprimes", countersCache)
  }

  def lastPrime(): Option[CheckedValue[Long]] = {
    usingcache(transaction { dbLastPrime.map(conv) }, "lastprime", lastCache)
  }

  def lastNotPrime(): Option[CheckedValue[Long]] = {
    usingcache(transaction { dbLastNotPrime.map(conv) }, "lastnotprime", lastCache)
  }

  def check(num: Long): Option[CheckedValue[Long]] = {
    usingcache(transaction { dbCheck(num).map(conv) }, num, cachedValuesCache)
  }

  private def using[R, T <% { def close(): Unit }](make: => T)(proc: T => R): R = {
    var xopt: Option[T] = None
    try {
      xopt = Option(make)
      xopt.map(proc).get
    } finally {
      xopt.foreach { _.close }
    }
  }

  import javax.sql.DataSource
  def slowsqlcheck(num: Long, dbpool: Option[DataSource], secs: Long): Option[CheckedValue[Long]] = {
    for { dbp <- dbpool } yield using(dbp.getConnection()) { conn =>
      using(conn.createStatement()) { stmt =>
        val sql = s"""select *,sleep($secs) as dummy from CachedValue where value = $num"""
        using(stmt.executeQuery(sql)) { rs =>
          rs.next()
          val isPrime = rs.getBoolean("isPrime")
          val digitCount = rs.getLong("digitCount")
          val nth = rs.getLong("nth")
          CheckedValue[Long](num, isPrime, digitCount, nth)
        }
      }
    }
  }

  def getPrime(nth: Long): Option[CheckedValue[Long]] = {
    usingcache(transaction { dbGetPrime(nth).map(conv) }, nth, primesCache)
  }

  def listPrimes(below: Long, above: Long) = {
    // the Iterator must be converted (evaluated) before exiting the transaction
    // this is done by the .toList.
    // Because whhen exiting the transaction, the result set is closed !
    transaction { dbListPrimes(below, above).toList }.map(conv)
  }

  private def ulam(sz: Int) = {
    val pgen = new PrimesGenerator[Long]
    transaction {
      val it = dbList(sz * sz).map(conv)
      val img = pgen.ulamSpiral(sz, it)
      img
    }
  }

  def ulamAsPNG(sz: Int): Array[Byte] = {
    usingcache({
      import javax.imageio.ImageIO
      import java.io._
      val bufferedImage = ulam(sz)
      val out = new ByteArrayOutputStream()
      ImageIO.write(bufferedImage, "PNG", out)
      out.toByteArray
    }, sz, ulamCache)
  }

  // TODO TO FINISH
  private val pgen = new PrimesGenerator[Long]
  def factorize(num: Long): Option[List[Long]] = {
    usingcache(pgen.factorize(num, pgen.primes.iterator), num, factorsCache)
  }

}
