package com.evolutiongaming.prometheus.cassandra;

import com.datastax.driver.core.Cluster;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import io.prometheus.client.Collector;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraDriverMetricsCollectorTest {
  private List<Cluster> clients = new ArrayList<>();

  @BeforeClass
  public static void startCassandra() throws IOException, TTransportException {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra("test-cassandra.yml");
  }

  @After
  public void stopClients() {
    clients.forEach(Cluster::close);
    clients.clear();
  }

  @Test
  public void shouldHandleNullMetrics() {
    Cluster clusterMock = mock(Cluster.class);
    when(clusterMock.getMetrics()).thenReturn(null);
    CassandraDriverMetricsCollector collector = new CassandraDriverMetricsCollector();
    collector.addClient("client1", clusterMock);

    List<Collector.MetricFamilySamples> result = collector.collect();
    assertValidResultNoSamples(result);
  }

  @Test
  public void addClient() {
    CassandraDriverMetricsCollector collector = new CassandraDriverMetricsCollector();

    collector.addClient("client1", connectClient());
    List<Collector.MetricFamilySamples> result = collector.collect();
    assertValidResultWithSamples(result, "client1");

    collector.addClient("client2", connectClient());
    result = collector.collect();
    assertValidResultWithSamples(result, "client1", "client2");
  }

  @Test
  public void removeClient() {
    CassandraDriverMetricsCollector collector = new CassandraDriverMetricsCollector();
    collector.addClient("client1", connectClient());
    collector.addClient("client2", connectClient());

    collector.removeClient("client1");
    List<Collector.MetricFamilySamples> result = collector.collect();
    assertValidResultWithSamples(result, "client2");
  }

  @Test
  public void clear() {
    CassandraDriverMetricsCollector collector = new CassandraDriverMetricsCollector();
    collector.addClient("client1", connectClient());
    collector.addClient("client2", connectClient());

    collector.clear();
    List<Collector.MetricFamilySamples> result = collector.collect();
    assertValidResultNoSamples(result);
  }

  private Cluster connectClient() {
    Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
    cluster.connect();
    clients.add(cluster);
    return cluster;
  }

  private void assertValidResultNoSamples(List<Collector.MetricFamilySamples> result) {
    assertValidResult(result);
    for (Collector.MetricFamilySamples mf : result) {
      assertEquals(0, mf.samples.size());
    }
  }

  private void assertValidResultWithSamples(
      List<Collector.MetricFamilySamples> result, String... clientNames
  ) {
    assertValidResult(result);
    List<Collector.MetricFamilySamples.Sample> samples = result.stream()
        .flatMap(mf -> mf.samples.stream()).collect(Collectors.toList());
    assertTrue(!samples.isEmpty());
    samples.forEach(sample -> {
      assertEquals("client", sample.labelNames.get(0));
      String clientName = sample.labelValues.get(0);
      assertTrue(Arrays.asList(clientNames).contains(clientName));
    });
  }

  private void assertValidResult(List<Collector.MetricFamilySamples> result) {
    assertNotNull(result);

    Set<String> seenMfNames = Sets.newLinkedHashSet();
    Set<PromSampleKey> seenSampleKeys = Sets.newLinkedHashSet();
    for (Collector.MetricFamilySamples mf : result) {
      assertNotNull(mf);

      assertNotNull(mf.name);
      assertTrue(mf.name.startsWith("cassandra_driver_"));
      assertTrue(!seenMfNames.contains(mf.name));
      seenMfNames.add(mf.name);

      assertNotNull(mf.type);

      assertNotNull(mf.help);
      assertNotNull(mf.samples);
      for (Collector.MetricFamilySamples.Sample sample : mf.samples) {
        assertNotNull(sample);
        assertNotNull(sample.name);
        assertTrue(sample.name.startsWith(mf.name));
        assertNotNull(sample.labelNames);
        assertNotNull(sample.labelValues);
        assertEquals(sample.labelNames.size(), sample.labelValues.size());
        PromSampleKey sampleKey = new PromSampleKey(sample);
        assertTrue(!seenSampleKeys.contains(sampleKey));
        seenSampleKeys.add(sampleKey);
      }
    }
  }

  private static final class PromSampleKey {
    final String name;
    final SortedMap<String, String> labels;

    PromSampleKey(Collector.MetricFamilySamples.Sample sample) {
      this.name = sample.name;
      ImmutableSortedMap.Builder<String, String> labelsBuilder = ImmutableSortedMap.naturalOrder();
      for (int i = 0; i < sample.labelNames.size(); ++i) {
        labelsBuilder.put(sample.labelNames.get(i), sample.labelValues.get(i));
      }
      this.labels = labelsBuilder.build();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PromSampleKey that = (PromSampleKey) o;
      return Objects.equal(name, that.name) &&
          Objects.equal(labels, that.labels);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, labels);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("name", name)
          .add("labels", labels)
          .toString();
    }
  }
}
