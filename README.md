# primes-scalatra-app #

## Build & Run ##

```sh
$ cd primes-scalatra-app
$ ./sbt
> container:start
> browse
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

##Requires a datasource named "primesdb"##
With tomcat
```
```

##Requires the following primes database (mysql or mariadb)##
```sql
mysql -uroot

DROP DATABASE primes;
CREATE DATABASE primes;
USE primes;
DROP USER 'tomcat';
CREATE USER 'tomcat';

GRANT ALL PRIVILEGES ON primes.* TO tomcat@localhost
    IDENTIFIED BY 'tomcat'
    WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON primes.* TO tomcat@10.134.115.167
    IDENTIFIED BY 'tomcat'
    WITH GRANT OPTION;
FLUSH PRIVILEGES;

SET PASSWORD FOR tomcat = PASSWORD('tomcat');

USE primes;
DROP TABLE CheckedValues;
CREATE TABLE CheckedValues (
   value BIGINT NOT NULL PRIMARY KEY,
   isPrime BOOLEAN NOT NULL,
   digitCount BIGINT NOT NULL,
   nth BIGINT NOT NULL
);

```