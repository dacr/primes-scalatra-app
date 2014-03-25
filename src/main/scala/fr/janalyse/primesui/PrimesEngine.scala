package fr.janalyse.primesui

import fr.janalyse.primes._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

import org.squeryl.PrimitiveTypeMode._


object PrimesEngine extends PrimesDBApi {
  private val pgen = new PrimesGenerator[Long]
  
  private val values = pgen.checkedValues

  
  private def populatePrimesIfRequired(upTo: Long = 100000) = {
    //val values = db("values")

    val fall = future {
      var resumedStream = pgen.checkedValues(lastPrime, lastNotPrime)
      while (resumedStream.head.value <= upTo) {
        //values.insert(resumedStream.head)
        transaction {
          val nv = resumedStream.head
          dbAddValue(conv(nv))
        }
        resumedStream = resumedStream.tail
      }
      'done
    }
    fall.onFailure {
      case x => println(s"NOK - ${x.getMessage()} - add indexes on values collection")
    }
    fall
  }

  var worker:Option[Future[Symbol]]=None
  
  def populate(upTo:Long) = this.synchronized {
    if (worker.isEmpty || worker.get.isCompleted) {
      val populateFuture = populatePrimesIfRequired(upTo)
      worker = Some(populateFuture)
      'Started
    } else 'StillInProgress
  }
  
  def conv(nv:CheckedValue[Long]):CachedValue = {
    new CachedValue(nv.value, nv.isPrime, nv.digitCount, nv.nth)
  }
  def conv(cv:CachedValue):CheckedValue[Long] = {
    new CheckedValue[Long](cv.value, cv.isPrime, cv.digitCount, cv.nth)
  }
  
  
  def valuesCount():Long = transaction { dbValuesCount()}
  
  def primesCount():Long = transaction {dbPrimesCount() }
  
  def notPrimesCount():Long = transaction {dbNotPrimesCount()}
  
  def lastPrime():Option[CheckedValue[Long]] = transaction {dbLastPrime.map(conv)}
  
  def lastNotPrime():Option[CheckedValue[Long]] = transaction {dbLastNotPrime.map(conv)}
  
  def check(num:Long):Option[CheckedValue[Long]] = transaction {dbCheck(num).map(conv)}

  def getPrime(nth:Long):Option[CheckedValue[Long]] = transaction {dbGetPrime(nth).map(conv)}
  
  // TODO TO FINISH
  def factorize(num:Long):Option[List[Long]] = pgen.factorize(num, pgen.primes.iterator)
  
}
