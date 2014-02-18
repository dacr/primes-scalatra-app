package fr.janalyse.primesui

import fr.janalyse.primes._

object PrimesEngine {
  private val pgen = new PrimesGenerator[Long]
  
  private val values = pgen.checkedValues

  
  def valuesCount() = -1
  
  def primesCount() = -1
  
  def notPrimesCount() = -1
  
  def lastPrime():Option[CheckedValue[Long]] = None
  
  def lastNotPrime():Option[CheckedValue[Long]] = None
  
  def check(num:Long) = values.find(_.value == num)

}