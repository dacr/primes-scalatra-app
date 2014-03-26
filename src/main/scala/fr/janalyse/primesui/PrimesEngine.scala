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

trait PrimesEngineMBean {  
  def isUseCache():Boolean
  def setUseCache(v:Boolean)
}

object PrimesEngine {
  val KEY="primes-engine"
}

class PrimesEngine extends PrimesDBApi with PrimesEngineMBean {
  
  private val oname =  s"primes:name=PrimesEngine"
  
  def setup() {
	JMX.register(this, oname)
    CacheManager.create()
    val manager = CacheManager.newInstance();
    val mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
  }
  
  def teardown() {
    JMX.unregister(oname)
    CacheManager.getInstance().shutdown()
  }
  
  private var useCaches=false
  def isUseCache():Boolean = useCaches
  def setUseCache(v:Boolean) {useCaches=v}

  
  private lazy val cache = CacheManager.getInstance()
  private lazy val cachedValuesCache = cache.getCache("CachedValuesCache")
  private lazy val countersCache = cache.getCache("CountersCache")
  private lazy val primesCache = cache.getCache("PrimesCache")
  private lazy val lastCache = cache.getCache("LastCache")
  private lazy val factorsCache = cache.getCache("FactorsCache")
  
  
  private def populatePrimesIfRequired(upTo: Long = 100000) = {
    val pgen = new PrimesGenerator[Long]
    val fall = future {
      var resumedStream = transaction {
        pgen.checkedValues(dbLastPrime.map(conv), dbLastNotPrime.map(conv))
      }
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
    } else {
      'StillInProgress
    }
  }

  def usingcache[T](mkresult: =>T, key:Any, cache:Cache):T = {
    if (!useCaches) mkresult
    else {
	    val item = cache.get(key)
	    if (item==null) {
	         val cv = mkresult
	         cache.put(new Element(key, cv))
	         cv
	    } else item.getObjectValue().asInstanceOf[T]
    }
  }
  
  def valuesCount():Long = {
    usingcache(transaction { dbValuesCount()}, "values", countersCache)
  }
  
  def primesCount():Long = {
    usingcache(transaction {dbPrimesCount() }, "primes", countersCache)
  }
  
  def notPrimesCount():Long = {
    usingcache(transaction {dbNotPrimesCount()}, "notprimes", countersCache)
  }
  
  def lastPrime():Option[CheckedValue[Long]] = {
    usingcache(transaction {dbLastPrime.map(conv)}, "lastprime", lastCache)
  }
  
  def lastNotPrime():Option[CheckedValue[Long]] = {
    usingcache(transaction {dbLastNotPrime.map(conv)}, "lastnotprime", lastCache)
  }
  
  def check(num:Long):Option[CheckedValue[Long]] = {
    usingcache(transaction {dbCheck(num).map(conv)}, num, cachedValuesCache)
  }

  def getPrime(nth:Long):Option[CheckedValue[Long]] = {
    usingcache(transaction {dbGetPrime(nth).map(conv)}, nth, primesCache)
  }
  
  // TODO TO FINISH
  private val pgen = new PrimesGenerator[Long]
  def factorize(num:Long):Option[List[Long]] = {
    usingcache(pgen.factorize(num, pgen.primes.iterator), num, factorsCache)
  }
  
}
