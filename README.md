# prometheus-cassandra-driver
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.org/evolution-gaming/prometheus-cassandra-driver.svg?branch=master)](https://travis-ci.org/evolution-gaming/prometheus-cassandra-driver)
[![version](https://api.bintray.com/packages/evolutiongaming/maven/prometheus-cassandra-driver/images/download.svg) ](https://bintray.com/evolutiongaming/maven/prometheus-cassandra-driver/_latestVersion)

Idiomatic Prometheus collector for Cassandra Java driver metrics.

It exposes the
[built-in metrics](https://docs.datastax.com/en/developer/java-driver/3.5/manual/metrics/)
in a idiomatic Prometheus way:
- Prometheus naming conventions are used
- where it is appropriate metrics are grouped under the same name with different set of labels
- multiple client instances per one JVM are supported and differentiated using the `client` label

## Usage

Add Evolution Gaming Bintray Maven repository to your artifact resolution:
* Maven:
```xml
<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>bintray-evolutiongaming-maven</id>
        <name>bintray</name>
        <url>https://dl.bintray.com/evolutiongaming/maven</url>
    </repository>
</repositories>
```
* SBT:
```scala
resolvers += Resolver.bintrayRepo("evolutiongaming", "maven")
```

Add the dependency:
* Maven:
```xml
<dependency>
  <groupId>com.evolutiongaming</groupId>
  <artifactId>prometheus-cassandra-driver</artifactId>
  <version>0.6</version>
</dependency>
```
* SBT:
```scala
libraryDependencies += "com.evolutiongaming" % "prometheus-cassandra-driver" % "0.6"
```

Example:
```java
Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
CassandraDriverMetricsCollector collector = new CassandraDriverMetricsCollector().register();
collector.addClient("global", cluster);
Session session = cluster.connect();
session.execute("select release_version from system.local;");
```
Resulting metrics:
```
# HELP cassandra_driver_known_hosts The number of Cassandra hosts currently known by the driver
# TYPE cassandra_driver_known_hosts gauge
cassandra_driver_known_hosts{client="global",} 1.0
# HELP cassandra_driver_connected_to_hosts The number of Cassandra hosts the driver is currently connected to
# TYPE cassandra_driver_connected_to_hosts gauge
cassandra_driver_connected_to_hosts{client="global",} 1.0
# HELP cassandra_driver_open_connections The total number of currently opened connections to Cassandra hosts
# TYPE cassandra_driver_open_connections gauge
cassandra_driver_open_connections{client="global",} 2.0
# HELP cassandra_driver_trashed_connections The total number of currently trashed connections to Cassandra hosts
# TYPE cassandra_driver_trashed_connections gauge
cassandra_driver_trashed_connections{client="global",} 0.0
# HELP cassandra_driver_in_flight_requests The total number of in flight requests to Cassandra hosts
# TYPE cassandra_driver_in_flight_requests gauge
cassandra_driver_in_flight_requests{client="global",} 0.0
# HELP cassandra_driver_executor_queue_depth The number of queued up tasks in the main internal executor, or -1, if that number is unknown
# TYPE cassandra_driver_executor_queue_depth gauge
cassandra_driver_executor_queue_depth{client="global",} 0.0
# HELP cassandra_driver_blocking_executor_queue_depth The number of queued up tasks in the blocking executor, or -1, if that number is unknown
# TYPE cassandra_driver_blocking_executor_queue_depth gauge
cassandra_driver_blocking_executor_queue_depth{client="global",} 0.0
# HELP cassandra_driver_reconnection_scheduler_queue_size The size of the work queue for the reconnection executor, or -1, if that number is unknown
# TYPE cassandra_driver_reconnection_scheduler_queue_size gauge
cassandra_driver_reconnection_scheduler_queue_size{client="global",} 0.0
# HELP cassandra_driver_task_scheduler_queue_size The size of the work queue for the scheduled tasks executor, or -1, if that number is unknown
# TYPE cassandra_driver_task_scheduler_queue_size gauge
cassandra_driver_task_scheduler_queue_size{client="global",} 1.0
# HELP cassandra_driver_sent_bytes_total The number of bytes sent so far
# TYPE cassandra_driver_sent_bytes_total counter
cassandra_driver_sent_bytes_total{client="global",} 754.0
# HELP cassandra_driver_received_bytes_total The number of bytes received so far
# TYPE cassandra_driver_received_bytes_total counter
cassandra_driver_received_bytes_total{client="global",} 57578.0
# HELP cassandra_driver_errors_total Encountered error events
# TYPE cassandra_driver_errors_total counter
cassandra_driver_errors_total{client="global",error_type="connection-errors",} 0.0
cassandra_driver_errors_total{client="global",error_type="authentication-errors",} 0.0
cassandra_driver_errors_total{client="global",error_type="write-timeouts",} 0.0
cassandra_driver_errors_total{client="global",error_type="read-timeouts",} 0.0
cassandra_driver_errors_total{client="global",error_type="unavailables",} 0.0
cassandra_driver_errors_total{client="global",error_type="client-timeouts",} 0.0
cassandra_driver_errors_total{client="global",error_type="other-errors",} 0.0
cassandra_driver_errors_total{client="global",error_type="retries",} 0.0
cassandra_driver_errors_total{client="global",error_type="retries-on-write-timeout",} 0.0
cassandra_driver_errors_total{client="global",error_type="retries-on-read-timeout",} 0.0
cassandra_driver_errors_total{client="global",error_type="retries-on-unavailable",} 0.0
cassandra_driver_errors_total{client="global",error_type="retries-on-client-timeout",} 0.0
cassandra_driver_errors_total{client="global",error_type="retries-on-connection-error",} 0.0
cassandra_driver_errors_total{client="global",error_type="retries-on-other-errors",} 0.0
cassandra_driver_errors_total{client="global",error_type="ignores",} 0.0
cassandra_driver_errors_total{client="global",error_type="ignores-on-write-timeout",} 0.0
cassandra_driver_errors_total{client="global",error_type="ignores-on-read-timeout",} 0.0
cassandra_driver_errors_total{client="global",error_type="ignores-on-unavailable",} 0.0
cassandra_driver_errors_total{client="global",error_type="ignores-on-client-timeout",} 0.0
cassandra_driver_errors_total{client="global",error_type="ignores-on-connection-error",} 0.0
cassandra_driver_errors_total{client="global",error_type="ignores-on-other-errors",} 0.0
cassandra_driver_errors_total{client="global",error_type="speculative-executions",} 0.0
# HELP cassandra_driver_request_time_seconds Exposes the rate and latency for user requests
# TYPE cassandra_driver_request_time_seconds untyped
cassandra_driver_request_time_seconds{client="global",quantile="0",} 0.016705277
cassandra_driver_request_time_seconds{client="global",quantile="0.5",} 0.016705277
cassandra_driver_request_time_seconds{client="global",quantile="0.75",} 0.016705277
cassandra_driver_request_time_seconds{client="global",quantile="0.95",} 0.016705277
cassandra_driver_request_time_seconds{client="global",quantile="0.98",} 0.016705277
cassandra_driver_request_time_seconds{client="global",quantile="0.99",} 0.016705277
cassandra_driver_request_time_seconds{client="global",quantile="0.999",} 0.016705277
cassandra_driver_request_time_seconds{client="global",quantile="1",} 0.016705277
cassandra_driver_request_time_seconds_count{client="global",} 1.0
cassandra_driver_request_time_seconds_mean{client="global",} 0.016705277
```
