package com.alibaba.fescar.metrics;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultGauge {
  @Test
  public void test() {
    AtomicLong value = new AtomicLong(0);

    Clock clock = new FixedClock(1000, 2000);
    Gauge<Long> gauge = new DefaultGauge<>(new Id("test_gauge").withTag("x", "a").withTag("y", "b"),
        value::incrementAndGet, clock);

    //direct get

    Assert.assertEquals(1, gauge.get(), 0);

    //measure

    List<Measurement> measurements = StreamSupport.stream(gauge.measure().spliterator(), false)
        .collect(Collectors.toList());
    Assert.assertEquals(1, measurements.size());
    Measurement measurement = measurements.get(0);
    Assert.assertEquals("test_gauge(x=a,y=b)", measurement.getId().toString());
    Assert.assertEquals(1000, measurement.getTimestamp(), 0);
    Assert.assertEquals(2, measurement.getValue(), 0);

    //measure again

    measurements = StreamSupport.stream(gauge.measure().spliterator(), false)
        .collect(Collectors.toList());
    Assert.assertEquals(1, measurements.size());
    measurement = measurements.get(0);
    Assert.assertEquals("test_gauge(x=a,y=b)", measurement.getId().toString());
    Assert.assertEquals(2000, measurement.getTimestamp(), 0);
    Assert.assertEquals(3, measurement.getValue(), 0);
  }
}
