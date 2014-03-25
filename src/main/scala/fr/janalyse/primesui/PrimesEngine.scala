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

trait PrimesCacheInit {
  def cacheSetup() {
    CacheManager.create()
    val manager = CacheManager.newInstance();
    val mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
  }
  
  def cacheTeardown() {
    CacheManager.getInstance().shutdown()
  }
}


object PrimesEngine extends PrimesDBApi {
  
  private lazy val cache = CacheManager.getInstance()
  private lazy val cachedValueCache = cache.getCache("CachedValue")
  
  private def populatePrimesIfRequired(upTo: Long = 100000) = {
    val pgen = new PrimesGenerator[Long]
    val fall = future {
      var resumedStream = pgen.checkedValues(lastPrime, lastNotPrime)
      while (resumedStream.head.value <= upTo) {
        transaction { dbAddValue(conv(resumedStream.head)) }
        resumedStream = resumedStream.tail
      }
      'done
    }
    fall.onFailure {
      case x => println(s"NOK - ${x.getMessage()} - add indexes on values collection")
    }
    fall
  }

  def conv(nv:CheckedValue[Long]):CachedValue = {
    new CachedValue(nv.value, nv.isPrime, nv.digitCount, nv.nth)
  }
  def conv(cv:CachedValue):CheckedValue[Long] = {
    new CheckedValue[Long](cv.value, cv.isPrime, cv.digitCount, cv.nth)
  }
  

  var worker:Option[Future[Symbol]]=None
  
  def populate(upTo:Long) = this.synchronized {
    if (worker.isEmpty || worker.get.isCompleted) {
      val populateFuture = populatePrimesIfRequired(upTo)
      worker = Some(populateFuture)
      'Started
    } else 'StillInProgress
  }
  
  def valuesCount():Long = transaction { dbValuesCount()}
  
  def primesCount():Long = transaction {dbPrimesCount() }
  
  def notPrimesCount():Long = transaction {dbNotPrimesCount()}
  
  def lastPrime():Option[CheckedValue[Long]] = transaction {dbLastPrime.map(conv)}
  
  def lastNotPrime():Option[CheckedValue[Long]] = transaction {dbLastNotPrime.map(conv)}
  
  //def check(num:Long):Option[CheckedValue[Long]] = transaction {dbCheck(num).map(conv)}
  
  def check(num:Long):Option[CheckedValue[Long]] = {
    val item = cachedValueCache.get(num)
    if (item==null) {
         val cv = transaction {dbCheck(num).map(conv)}
         cachedValueCache.put(new Element(num, cv))
         cv
    } else item.getObjectValue().asInstanceOf[Option[CheckedValue[Long]]]
  }

  def getPrime(nth:Long):Option[CheckedValue[Long]] = transaction {dbGetPrime(nth).map(conv)}
  
  // TODO TO FINISH
  val pgen = new PrimesGenerator[Long]
  def factorize(num:Long):Option[List[Long]] = pgen.factorize(num, pgen.primes.iterator)
  
}
