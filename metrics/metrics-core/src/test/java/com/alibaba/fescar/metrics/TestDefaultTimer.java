package com.alibaba.fescar.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultTimer {
  @Test
  public void test() {
    Clock clock = new FixedClock(1000, 2000);
    Timer timer = new DefaultTimer(new Id("test_timer").withTag("x", "a").withTag("y", "b"), clock);

    timer.record(1, TimeUnit.SECONDS);
    timer.record(1000, TimeUnit.MILLISECONDS);
    timer.record(1000 * 1000 + 3, TimeUnit.MICROSECONDS);

    //direct get

    Assert.assertEquals(3, timer.count());
    Assert.assertEquals(3 * 1000 * 1000 + 3, timer.total());
    Assert.assertEquals(1000 * 1000 + 3, timer.max());
    Assert.assertEquals(1000 * 1000 + 1, timer.average(), 0);

    //measure

    Map<String, Measurement> measurements = new HashMap<>();
    timer.measure().forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));
    Assert.assertEquals(4, measurements.size());

    Measurement measurement = measurements.get("test_timer(statistic=count,x=a,y=b)");
    Assert.assertEquals(1000, measurement.getTimestamp(), 0);
    Assert.assertEquals(3, measurement.getValue(), 0);

    measurement = measurements.get("test_timer(statistic=total,x=a,y=b)");
    Assert.assertEquals(1000, measurement.getTimestamp(), 0);
    Assert.assertEquals(3000.003, measurement.getValue(), 0);

    measurement = measurements.get("test_timer(statistic=max,x=a,y=b)");
    Assert.assertEquals(1000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1000.003, measurement.getValue(), 0);

    measurement = measurements.get("test_timer(statistic=average,x=a,y=b)");
    Assert.assertEquals(1000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1000.001, measurement.getValue(), 0);

    //measure again

    timer.record(1, TimeUnit.SECONDS);

    measurements.clear();

    timer.measure().forEach(measurement2 -> measurements.put(measurement2.getId().toString(), measurement2));
    Assert.assertEquals(4, measurements.size());

    measurement = measurements.get("test_timer(statistic=count,x=a,y=b)");
    Assert.assertEquals(2000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1, measurement.getValue(), 0);

    measurement = measurements.get("test_timer(statistic=total,x=a,y=b)");
    Assert.assertEquals(2000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1000.0, measurement.getValue(), 0);

    measurement = measurements.get("test_timer(statistic=max,x=a,y=b)");
    Assert.assertEquals(2000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1000.0, measurement.getValue(), 0);

    measurement = measurements.get("test_timer(statistic=average,x=a,y=b)");
    Assert.assertEquals(2000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1000.0, measurement.getValue(), 0);
  }
}
