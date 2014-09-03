# primes-scalatra-app #

## Build & Run locally ##

```sh
$ cd primes-scalatra-app
$ ./sbt
> container:start
> browse
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

## Build & deploy to an application server  ##

```sh
$ cd primes-scalatra-app
$ ./sbt package
```
It will generate a file such as : 
*.../primes-scalatra-app/target/scala-2.10/primesui_2.10-0.1.0.war *
Just rename it to **primesui.war** and deploy it. For apache tomcat just drop this file to the following directory
**.../apache-tomcat/webapps/** 

## Requirements  ##

 - create the required mysql user and database, using such command :
 
   ```
   mysql -h localhost -uroot < initdb.sql
   ```

## Configuration ##

 * **PRIMESUI-CACHE** environment variable or java system properties can be used in order
   to control the initial state of the primes application cache (used or not used).
   If not set, the default value is false.

 * To specify a remote database, you can either use :
   - **PRIMES_DB_HOST** java system property
   - **PRIMES_DB_HOST** environment variable
   - **OPENSHIFT_MYSQL_DB_HOST** environment variable
   

