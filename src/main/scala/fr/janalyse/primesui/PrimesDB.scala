package fr.janalyse.primesui

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.annotations.Column
import java.util.Date
import java.sql.Timestamp

class CachedPrime(
  val value: Long,
  val isPrime: Boolean,
  val digitCount: Long,
  val nth: Long)

object PrimesDB extends Schema {
  val cachedPrimes = table[CachedPrime]

  on(cachedPrimes)(cp => declare(
    cp.value is (unique, indexed),
    cp.isPrime is (indexed),
    cp.nth is (unique, indexed)
  ))

}


object PrimesDBApi {
  import PrimesDB._
  
  def getPrime(value:Long):Option[CachedPrime] = 
    cachedPrimes.find(s =>s.value == value)
  
}
