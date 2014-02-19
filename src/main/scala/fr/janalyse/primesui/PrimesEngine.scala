package fr.janalyse.primesui

import fr.janalyse.primes._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._


object PrimesEngine {
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
  
  
  
  def valuesCount() = -1
  
  def primesCount() = -1
  
  def notPrimesCount() = -1
  
  def lastPrime():Option[CheckedValue[Long]] = None
  
  def lastNotPrime():Option[CheckedValue[Long]] = None
  
  def check(num:Long) = values.find(_.value == num)

  def getPrime(nth:Long) = pgen.checkedValues.filter(_.isPrime).find(_.nth == nth)
  
  def factorize(num:Long) = pgen.factorize(num, pgen.primes.iterator)
  
}
