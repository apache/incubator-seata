package com.alibaba.fescar.metrics;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultCounter {
  @Test
  public void test() {
    Clock clock = new FixedClock(1000, 2000);
    Counter counter = new DefaultCounter(new Id("test_counter").withTag("x", "a").withTag("y", "b"), clock);

    //direct get

    counter.increase(2);
    Assert.assertEquals(2, counter.get());
    counter.decrease(1);
    Assert.assertEquals(1, counter.get());

    //measure

    List<Measurement> measurements = StreamSupport.stream(counter.measure().spliterator(), false)
        .collect(Collectors.toList());
    Assert.assertEquals(1, measurements.size());
    Measurement measurement = measurements.get(0);
    Assert.assertEquals("test_counter(x=a,y=b)", measurement.getId().toString());
    Assert.assertEquals(1000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1, measurement.getValue(), 0);

    //measure again

    counter.increase(100);
    Assert.assertEquals(101, counter.get());

    measurements = StreamSupport.stream(counter.measure().spliterator(), false)
        .collect(Collectors.toList());
    Assert.assertEquals(1, measurements.size());
    measurement = measurements.get(0);
    Assert.assertEquals("test_counter(x=a,y=b)", measurement.getId().toString());
    Assert.assertEquals(2000, measurement.getTimestamp(), 0);
    Assert.assertEquals(101, measurement.getValue(), 0);
  }
}
