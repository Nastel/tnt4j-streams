# PostgreSQL (PG) statistics streaming sample

This sample executes SQL query (every 5 minutes) to select PG schemas defined tables statistics from PG statistics views 
`pg_stat_all_tables` and `pg_statio_all_tables`. 

Communication with database if performed using PG JDBC driver. 

## Pre-requisites

See [download instructions](lib/postgres/download.html) how to download PG JDBC driver jar manually and add it to `tnt4j-streams` package 
`lib` dir.

In case you compile `tnt4j-streams` on your own, then you can uncomment `runtime` scope PG driver dependency in `tnt4j-streams-ws/pom.xml` 
file: 
```xml
    <!-- PostgreSQL JDBC Drivers -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.2.23</version>
        <scope>runtime</scope>
    </dependency>
```

## Usage

* Change `[PG_HOST]` to your PG DB host/IP
    * default post is set to `5432`. In case your DB port is different - change it accordingly
* Change `[PG_USER_PASS]` to your PG user password
    * default user name is set to `postgres`. In case your DB user is different - change it accordingly
