
DROP DATABASE IF EXISTS primes;

CREATE DATABASE primes;
GRANT ALL PRIVILEGES ON primes.* TO 'optimus'@'localhost' identified by 'bumblebee' ;
FLUSH PRIVILEGES;

USE PRIMES;

CREATE TABLE CachedPrime(
  value      BIGINT  NOT NULL PRIMARY KEY,
  isPrime    BOOLEAN NOT NULL,
  digitCount BIGINT  NOT NULL,
  nth        BIGINT  NOT NULL
);

CREATE INDEX cachedPrimeIDX ON CachedPrime(isPrime, nth);
