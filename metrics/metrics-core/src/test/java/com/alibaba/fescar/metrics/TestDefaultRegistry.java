package com.alibaba.fescar.metrics;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultRegistry {
  @Test
  public void test() {
    Registry registry = new DefaultRegistry();

    Id id1 = new Id("id1");
    Timer timer = registry.getTimer(id1);

    Id id2 = new Id("id2");
    Counter counter = registry.getCounter(id2);

    Id id3 = new Id("id3");
    Gauge<Long> gauge = registry.getGauge(id3, () -> 0L);

    Id id4 = new Id("id4");
    Summary summary = registry.getSummary(id4);

    try {
      registry.getCounter(id1);
      Assert.fail("must throw exception");
    } catch (Exception ignored) {
    }
  }
}
