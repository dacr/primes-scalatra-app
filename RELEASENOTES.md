
# 0.1.19-SNAPSHOT after 2015-11-07

 * moved from scalate to twirl because of performance issues within scalate linked with class loader
 * sysinfo reporting improvements and small refactoring
 * new problem simulation urls :
   - sessionleakedcheck : memory leak within the current user session
   - jdbcleakcheck : to loose one jdbc connection

# 0.1.19-SNAPSHOT before 2015-11-07
 
 * scalatra 2.4.0-RC3
 * c3p0 downgraded to 0.9.2.1 
   - problem with java7 it uses new java8 jdbc API =>
     java.lang.NoClassDefFoundError: java/sql/SQLType
   - [issues/57](https://github.com/swaldman/c3p0/issues/57)
 * scalatra scalate templates are now used
   - scaml base templates
   - isDevelopmentMode set to false
   - templates are precompiled
 * twitter bootstrap is now used
 * more logging added to improve admin operations


# 0.1.3-SNAPSHOT

 * updates :
   - scalatra 2.3.1
   - sbt 0.13.9
   - ehcache-core 2.6.11
   - c3p0 0.9.5.1
   - mysql-connector-java 5.1.36
   - scala 2.11.7
   - logback-classic 1.1.3
 * logs added


# 0.1.2 (2015-04-08)

 * Externally data source defined support added, through "jdbc/primesui" jndi name


# 0.1.1 (2015-02-02)

 * dbLastPrime & dbLastNotPrime memory usage fixed : https://github.com/dacr/primes-scalatra-app/issues/1
 * ehcache improved default cache values
 * scalatra 2.3.0
 * scala 2.11.5
 * mysql-connector-java 5.1.34
 * logback 1.1.2
 * ehcache-core 2.6.10
 * squeryl 0.9.5-7
 * c3p0 0.9.5
 * janalyse-jmx 0.7.1
 * primes 1.2.1
 * jetty-webapp 8.1.16.v20140903
 * javax.servlet.jsp 2.2.0.v201112011158 added to avoid exception with container:start
 
