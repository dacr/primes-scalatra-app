
DROP DATABASE IF EXISTS primes;

CREATE DATABASE primes;
GRANT ALL PRIVILEGES ON primes.* TO 'optimus'@'localhost' identified by 'bumblebee' ;
FLUSH PRIVILEGES;

USE primes;

CREATE TABLE CachedValue(
  value      BIGINT  NOT NULL PRIMARY KEY,
  isPrime    BOOLEAN NOT NULL,
  digitCount BIGINT  NOT NULL,
  nth        BIGINT  NOT NULL
);

CREATE INDEX cachedValueIDX ON CachedValue(isPrime, nth);


# TO connect from command line :
# mysql -h localhost -u optimus -p primes
#
#
