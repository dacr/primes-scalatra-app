
DROP DATABASE IF EXISTS primes;

CREATE DATABASE primes;
GRANT ALL PRIVILEGES ON primes.* TO 'optimus'@'localhost' identified by 'bumblebee' ;
FLUSH PRIVILEGES;

USE PRIMES;

CREATE TABLE CachedPrime(
  value      BIGINT,
  isPrime    BOOLEAN,
  digitCount BIGINT,
  nth        BIGINT
);

