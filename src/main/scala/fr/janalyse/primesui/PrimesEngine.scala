package fr.janalyse.primesui

import fr.janalyse.primes._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._


object PrimesEngine extends PrimesDBApi {
  private val pgen = new PrimesGenerator[Long]
  
  private val values = pgen.checkedValues

  
  private def populatePrimesIfRequired(upTo: Long = 100000) = {
    //val values = db("values")

    val fall = future {
      var resumedStream = pgen.checkedValues(lastPrime, lastNotPrime)
      while (resumedStream.head.value <= upTo) {
        //values.insert(resumedStream.head)
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
      concurrent.future {'JobStarted}
    } else concurrent.future {'StillInProgress}
  }
  
  def conv(cv:CachedValue):CheckedValue[Long] = {
    new CheckedValue[Long](cv.value, cv.isPrime, cv.digitCount, cv.nth)
  }
  
  
  def valuesCount():Long = dbValuesCount()
  
  def primesCount():Long = dbPrimesCount()
  
  def notPrimesCount():Long = dbNotPrimesCount()
  
  def lastPrime():Option[CheckedValue[Long]] = dbLastPrime.map(conv)
  
  def lastNotPrime():Option[CheckedValue[Long]] = dbLastNotPrime.map(conv)
  
  def check(num:Long):Option[CheckedValue[Long]] = dbCheck(num).map(conv)

  def getPrime(nth:Long):Option[CheckedValue[Long]] = dbGetPrime(nth).map(conv)
  
  // TODO TO FINISH
  def factorize(num:Long):Option[List[Long]] = pgen.factorize(num, pgen.primes.iterator)
  
}
