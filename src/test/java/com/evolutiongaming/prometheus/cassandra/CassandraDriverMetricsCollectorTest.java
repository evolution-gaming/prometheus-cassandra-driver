package com.evolutiongaming.prometheus.cassandra;

import com.datastax.driver.core.Cluster;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import io.prometheus.client.Collector;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraDriverMetricsCollectorTest {
  @Test
  public void shouldHandleNullMetrics() {
    Cluster clusterMock = mock(Cluster.class);
    when(clusterMock.getMetrics()).thenReturn(null);
    CassandraDriverMetricsCollector collector = new CassandraDriverMetricsCollector();
    collector.addClient("client1", clusterMock);

    List<Collector.MetricFamilySamples> result = collector.collect();
    assertValidResultNoSamples(result);
  }

  private void assertValidResultNoSamples(List<Collector.MetricFamilySamples> result) {
    assertValidResult(result);
    for (Collector.MetricFamilySamples mf : result) {
      assertEquals(0, mf.samples.size());
    }
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
