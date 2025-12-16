package com.evolutiongaming.prometheus.cassandra;

import com.codahale.metrics.Counter;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metrics;
import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Exports Java Cassandra Driver metrics to Prometheus in an idiomatic way, with labels and all the goodies.
 * <p>
 * Multiple client instances per one JVM are supported and differentiated using the {@code client} label.
 * <p>
 * Example:
 * <pre>{@code
 *
 * Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
 * CassandraDriverMetricsCollector collector = new CassandraDriverMetricsCollector().register();
 * collector.addClient("global", cluster);
 *
 * }</pre>
 * <p>
 * All the exported metrics names start with {@code cassandra_driver_} prefix.
 *
 * @see <a href="https://docs.datastax.com/en/developer/java-driver/3.5/manual/metrics/">Java Cassandra Driver metrics description</a>
 */
public class CassandraDriverMetricsCollector extends Collector {
  private static final List<String> BASE_LABEL_NAMES = Collections.singletonList("client");

  private final ConcurrentMap<String, Cluster> clients = new ConcurrentHashMap<>();

  /**
   * Add or replace the client instance with the given name.
   * <p>
   * Any references any previous instance with this name is invalidated.
   *
   * @param clientName The name of the client instance, will be the metrics label value
   * @param cluster    The client instance cluster object being monitored
   */
  @SuppressWarnings("WeakerAccess")
  public void addClient(String clientName, Cluster cluster) {
    clients.put(clientName, cluster);
  }

  /**
   * Remove the client instance with the given name.
   * <p>
   * Any references to the instance are invalidated.
   *
   * @param clientName client instance to be removed
   */
  public void removeClient(String clientName) {
    clients.remove(clientName);
  }

  /**
   * Remove all client instances.
   * <p>
   * Any references to all clients are invalidated.
   */
  public void clear() {
    clients.clear();
  }

  @Override
  public List<MetricFamilySamples> collect() {
    return new ResultBuilder().build();
  }

  private class ResultBuilder {
    private final List<MetricFamilySamples> mfs = new ArrayList<>();

    private final TimerMetricFamilyBuilder requestTimeBuilder = new TimerMetricFamilyBuilder(
        "cassandra_driver_request_time_seconds",
        "Exposes the rate and latency for user requests",
        BASE_LABEL_NAMES
    );

    private final GaugeMetricFamily knownHosts = createGauge(
        "cassandra_driver_known_hosts",
        "The number of Cassandra hosts currently known by the driver"
    );
    private final GaugeMetricFamily connectedToHosts = createGauge(
        "cassandra_driver_connected_to_hosts",
        "The number of Cassandra hosts the driver is currently connected to"
    );
    private final GaugeMetricFamily openConnections = createGauge(
        "cassandra_driver_open_connections",
        "The total number of currently opened connections to Cassandra hosts"
    );
    private final GaugeMetricFamily trashedConnections = createGauge(
        "cassandra_driver_trashed_connections",
        "The total number of currently trashed connections to Cassandra hosts"
    );
    private final GaugeMetricFamily inFlightRequests = createGauge(
        "cassandra_driver_in_flight_requests",
        "The total number of in flight requests to Cassandra hosts"
    );
    private final GaugeMetricFamily requestQueueDepth = createGauge(
        "cassandra_driver_request_queue_depth",
        "The total number of enqueued requests on all Cassandra hosts"
    );
    private final GaugeMetricFamily executorQueueDepth = createGauge(
        "cassandra_driver_executor_queue_depth",
        "The number of queued up tasks in the main internal executor, or -1, if that number is unknown"
    );
    private final GaugeMetricFamily blockingExecutorQueueDepth = createGauge(
        "cassandra_driver_blocking_executor_queue_depth",
        "The number of queued up tasks in the blocking executor, or -1, if that number is unknown"
    );
    private final GaugeMetricFamily reconnectionSchedulerQueueSize = createGauge(
        "cassandra_driver_reconnection_scheduler_queue_size",
        "The size of the work queue for the reconnection executor, or -1, if that number is unknown"
    );
    private final GaugeMetricFamily taskSchedulerQueueSize = createGauge(
        "cassandra_driver_task_scheduler_queue_size",
        "The size of the work queue for the scheduled tasks executor, or -1, if that number is unknown"
    );
    private final CounterMetricFamily bytesSent = createCounter(
        "cassandra_driver_sent_bytes_total",
        "The number of bytes sent so far"
    );
    private final CounterMetricFamily bytesReceived = createCounter(
        "cassandra_driver_received_bytes_total",
        "The number of bytes received so far"
    );
    private final CounterMetricFamily errors = createCounter(
        "cassandra_driver_errors_total",
        "Encountered error events",
        "error_type"
    );

    private GaugeMetricFamily createGauge(String name, String help) {
      GaugeMetricFamily mf = new GaugeMetricFamily(name, help, BASE_LABEL_NAMES);
      mfs.add(mf);
      return mf;
    }

    private CounterMetricFamily createCounter(String name, String help, String... additionalLabels) {
      List<String> labelNames;
      if (additionalLabels == null || additionalLabels.length == 0) {
        labelNames = BASE_LABEL_NAMES;
      } else {
        labelNames = new ArrayList<>(BASE_LABEL_NAMES);
        labelNames.addAll(Arrays.asList(additionalLabels));
      }
      CounterMetricFamily mf = new CounterMetricFamily(name, help, labelNames);
      mfs.add(mf);
      return mf;
    }

    List<MetricFamilySamples> build() {
      for (Map.Entry<String, Cluster> entry : clients.entrySet()) {
        String clientName = entry.getKey();
        List<String> labels = Collections.singletonList(clientName);
        Metrics metrics = entry.getValue().getMetrics();

        if (metrics != null) {
          requestTimeBuilder.addTimerMetricSample(labels, metrics.getRequestsTimer());

          knownHosts.addMetric(labels, metrics.getKnownHosts().getValue());
          connectedToHosts.addMetric(labels, metrics.getConnectedToHosts().getValue());
          openConnections.addMetric(labels, metrics.getOpenConnections().getValue());
          trashedConnections.addMetric(labels, metrics.getTrashedConnections().getValue());
          inFlightRequests.addMetric(labels, metrics.getInFlightRequests().getValue());

          requestQueueDepth.addMetric(labels, metrics.getRequestQueueDepth().getValue());
          executorQueueDepth.addMetric(labels, metrics.getExecutorQueueDepth().getValue());
          blockingExecutorQueueDepth.addMetric(labels, metrics.getBlockingExecutorQueueDepth().getValue());
          reconnectionSchedulerQueueSize
              .addMetric(labels, metrics.getReconnectionSchedulerQueueSize().getValue());
          taskSchedulerQueueSize.addMetric(labels, metrics.getTaskSchedulerQueueSize().getValue());

          bytesSent.addMetric(labels, metrics.getBytesSent().getCount());
          bytesReceived.addMetric(labels, metrics.getBytesReceived().getCount());

          addErrorsMetrics(clientName, metrics.getErrorMetrics());
        }
      }
      mfs.addAll(requestTimeBuilder.build());
      return mfs;
    }

    private void addErrorsMetrics(String clientName, Metrics.Errors metrics) {
      addErrorMetric(clientName, "connection-errors", metrics.getConnectionErrors());
      addErrorMetric(clientName, "authentication-errors", metrics.getAuthenticationErrors());

      addErrorMetric(clientName, "write-timeouts", metrics.getWriteTimeouts());
      addErrorMetric(clientName, "read-timeouts", metrics.getReadTimeouts());
      addErrorMetric(clientName, "unavailables", metrics.getUnavailables());
      addErrorMetric(clientName, "client-timeouts", metrics.getClientTimeouts());

      addErrorMetric(clientName, "other-errors", metrics.getOthers());

      addErrorMetric(clientName, "retries", metrics.getRetries());
      addErrorMetric(clientName, "retries-on-write-timeout", metrics.getRetriesOnWriteTimeout());
      addErrorMetric(clientName, "retries-on-read-timeout", metrics.getRetriesOnReadTimeout());
      addErrorMetric(clientName, "retries-on-unavailable", metrics.getRetriesOnUnavailable());
      addErrorMetric(clientName, "retries-on-client-timeout", metrics.getRetriesOnClientTimeout());
      addErrorMetric(clientName, "retries-on-connection-error", metrics.getRetriesOnConnectionError());
      addErrorMetric(clientName, "retries-on-other-errors", metrics.getRetriesOnOtherErrors());

      addErrorMetric(clientName, "ignores", metrics.getIgnores());
      addErrorMetric(clientName, "ignores-on-write-timeout", metrics.getIgnoresOnWriteTimeout());
      addErrorMetric(clientName, "ignores-on-read-timeout", metrics.getIgnoresOnReadTimeout());
      addErrorMetric(clientName, "ignores-on-unavailable", metrics.getIgnoresOnUnavailable());
      addErrorMetric(clientName, "ignores-on-client-timeout", metrics.getIgnoresOnClientTimeout());
      addErrorMetric(clientName, "ignores-on-connection-error", metrics.getIgnoresOnConnectionError());
      addErrorMetric(clientName, "ignores-on-other-errors", metrics.getIgnoresOnOtherErrors());

      addErrorMetric(clientName, "speculative-executions", metrics.getSpeculativeExecutions());
    }

    private void addErrorMetric(String clientName, String type, Counter counter) {
      List<String> labelValues = Arrays.asList(clientName, type);
      errors.addMetric(labelValues, counter.getCount());
    }
  }
}
