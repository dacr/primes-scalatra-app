
DROP DATABASE IF EXISTS primes;

CREATE DATABASE primes;
GRANT ALL PRIVILEGES ON primes.* TO 'optimus'@'localhost' identified by 'bumblebee' ;
FLUSH PRIVILEGES;

# TO connect from command line :
# mysql -h localhost -u optimus -p primes
#
#
