
DROP DATABASE IF EXISTS primes;

CREATE DATABASE primes;
GRANT ALL PRIVILEGES ON primes.* TO 'optimus'@'localhost' identified by 'bumblebee' ;
FLUSH PRIVILEGES;

USE primes;

CREATE TABLE CachedPrime(
  value      BIGINT  NOT NULL PRIMARY KEY,
  isPrime    BOOLEAN NOT NULL,
  digitCount BIGINT  NOT NULL,
  nth        BIGINT  NOT NULL
);

CREATE INDEX cachedPrimeIDX ON CachedPrime(isPrime, nth);


# TO connect from command line :
# mysql -h localhost -u optimus -p primes
#
#
