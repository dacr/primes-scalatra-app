# primes-scalatra-app #

You can directly play with this application on this server  : [http://www.janalyse.org/primesui/](http://www.janalyse.org/primesui/), dangerous features (for the server) are disabled.

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
*.../primes-scalatra-app/target/scala-2.11/primesui_2.11-0.1.1.war*
Just rename it to **primesui.war** and deploy it. For apache tomcat just drop this file to the following directory
**.../apache-tomcat/webapps/** 

## Requirements  ##

 - The minimum is to have the user, its password and the database name, primesui can automatically create the tables and required indexes.
 - or create the required mysql user and database, using such command :
   ```
   mysql -h localhost -uroot < initdb.sql
   ```


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

 * To specify a remote database using an externally defined data source :
   - Define a data source linked to "jdbc/primesui" jndi name.
   - any available external data source is used in priority over the default internal data source (C3P0 based) 
   
 * To specify a remote database using the internal jdbc pool, you can either use :
   - **PRIMES_DB_HOST**, **PRIMES_DB_PORT** and **PRIMES_DB_NAME** java system properties OR environment variables
   - If not set, primesui will try to use, either : 
     - **OPENSHIFT_MYSQL_DB_HOST** and **OPENSHIFT_MYSQL_DB_PORT** environment variables (when used with Redhat openshift systems)
     - **RDS_HOSTNAME**, **RDS_PORT**, **RDS_USERNAME**, **RDS_PASSWORD** and **RDS_DB_NAME** environment variables (when used with AWS systems)
     - **DOCKER_PRIMES_DB_PORT_\d+_TCP_ADDR**, **DOCKER_PRIMES_DB_PORT_\d+_TCP_PORT** environment variables (when used with DOCKER systems)
   - defaults are : localhost, 3306 for the port, and primes for the database name



