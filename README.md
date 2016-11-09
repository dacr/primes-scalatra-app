# ![](http://www.janalyse.fr/primesui/logo.png) primesui #

Primesui is a high performance web application which let you play with primes number.
It can be deployed on any kind of application servers hosted on physical or virtual servers,
on docker containers, on amazon web services, on openshift, ...

With caching enabled you can reach thousands of simultaneous users, and thousands 
of requests served every seconds.
[7000 users, 11000 hit/s example](http://www.janalyse.fr/gatling/loadtest-7000vus/) 
with mysql, jetty, and gatling running on the same host (Linux gentoo, 6CPU, AMD II
phenom X6 1090T).

This demo application also comes with a set of special features dedicated to experiments.
Nine typical problems are simulated, from memory or jdbc connection leaks to poorly written
logs. 

This application relies on :
* [scala](http://www.scala-lang.org/) : the jvm language used.
* [scalatra](http://scalatra.org/) : the web framework.
* [twirl](https://github.com/playframework/twirl) : the templating engine.
* [squeryl](http://squeryl.org/) : the database layer
* [ehcache](http://www.ehcache.org/) : the second level object cache.
* [logback](http://logback.qos.ch/) : the logging system.

You can directly play with this application on this server :
[http://www.janalyse.org/primesui/](http://www.janalyse.org/primesui/),
experiments features are disabled. 

The latest `primesui.war` snapshot binary release (branch named `develop`) is available on
[http://www.janalyse.fr/primesui/primesui.war](http://www.janalyse.fr/primesui/primesui.war).
Use this binary release, or build it by yourself as described in the next section.

You can easily run your own load tests on primesui using this
[github project](https://github.com/dacr/primes-scalatra-app-loadtests). Just run `sbt test`
from this project directory and your load tests will start. Several environment variables
to configure or customize your test as described in the project page. 

## Build & Run locally ##

```sh
$ cd primes-scalatra-app
$ ./sbt
> jetty:start
> browse
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

## Build & deploy to an application server  ##

```sh
$ cd primes-scalatra-app
$ ./sbt package
```
It will generate a file such as : 
*target/scala-2.11/primesui.war*

For apache tomcat just drop this file to the following directory
**apache-tomcat/webapps/** 

## Requirements  ##

 * A running mysql database server. Without any configuration, primesui will try to connect to it using :
   - mysql host : `127.0.0.1`
   - mysql database : `primes`
   - mysql username : `optimus`
   - mysql password : `bumblebee`
 * Connect to your database : `mysql -h localhost -uroot -p`
   - Create the required mysql user and database, using such sql commands :
   ```
   CREATE DATABASE primes;
   GRANT ALL PRIVILEGES ON primes.* TO 'optimus'@'localhost' identified by 'bumblebee' ;
   FLUSH PRIVILEGES;
   ```
 * All database tables and indexes will be automatically created if needed

## Configuration ##

 * **PRIMESUI_CACHE** environment variable or java system properties can be used in order
   to control the initial state of the primes application cache (used or not used).
   If not set, the default value is false.
 * **PRIMESUI_TESTING** environment variable or java system property can be used to enable
   dangerous features that simulates problems (mem leak, slow requests, ...).
   If not set, the default value is true.
 * **PRIMESUI_SESSION** environment variable or java system property can be used to enable
   user sessions that will be used to store and show various user related data. This feature
   helps to test session affinity or application server clusters. If enabled, a message appears
   on the home page 'current user homepage hit count=x'
   If not set, the default is false.
 * **PRIMESUI_DEBUG** environment variable or java system property can be used to enable
   the DEBUG log level (if "true")
 * Database configuration
   * To specify a remote database using an externally defined data source :
     - Define a data source linked to "jdbc/primesui" jndi name.
     - any available external data source is used in priority over the default internal data source (C3P0 based) 
   * To specify a remote database using the internal jdbc pool (c3p0), you can either use :
     - **PRIMES_DB_HOST**, **PRIMES_DB_PORT** and **PRIMES_DB_NAME** java system properties OR environment variables
     - If not set, primesui will try to use, either : 
       - **OPENSHIFT_MYSQL_DB_HOST** and **OPENSHIFT_MYSQL_DB_PORT** environment variables (when used with Redhat openshift systems)
       - **RDS_HOSTNAME**, **RDS_PORT**, **RDS_USERNAME**, **RDS_PASSWORD** and **RDS_DB_NAME** environment variables (when used with AWS systems)
       - **DOCKER_PRIMES_DB_PORT_\d+_TCP_ADDR**, **DOCKER_PRIMES_DB_PORT_\d+_TCP_PORT** environment variables (when used with DOCKER systems)
     - defaults are : localhost, 3306 for the port, and primes for the database name
