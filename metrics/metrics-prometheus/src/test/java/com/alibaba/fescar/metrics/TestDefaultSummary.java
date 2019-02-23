package com.alibaba.fescar.metrics;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultSummary {
  @Test
  public void test() {

    Clock clock = new FixedClock(1000, 2000, 3000, 5000);
    Summary summary = new DefaultSummary(new Id("test_summary").withTag("x", "a").withTag("y", "b"), clock);

    //direct get

    //now the timestamp is 1000;
    summary.increase(1);
    Assert.assertEquals(1, summary.count());
    Assert.assertEquals(1, summary.total());
    Assert.assertEquals(1, summary.tps(), 0);

    //measure

    summary.increase(2);
    Map<String, Measurement> measurements = new HashMap<>();
    summary.measure().forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));
    Assert.assertEquals(3, measurements.size());

    Measurement measurement = measurements.get("test_summary(statistic=count,x=a,y=b)");
    Assert.assertEquals(3000, measurement.getTimestamp(), 0);
    Assert.assertEquals(2, measurement.getValue(), 0);

    measurement = measurements.get("test_summary(statistic=total,x=a,y=b)");
    Assert.assertEquals(3000, measurement.getTimestamp(), 0);
    Assert.assertEquals(3, measurement.getValue(), 0);

    // 3 / 2000 * 1000
    measurement = measurements.get("test_summary(statistic=tps,x=a,y=b)");
    Assert.assertEquals(3000, measurement.getTimestamp(), 0);
    Assert.assertEquals(1.5, measurement.getValue(), 0);

    summary.increase(3);
    summary.increase(3);

    //measure again

    measurements.clear();
    summary.measure().forEach(measurement2 -> measurements.put(measurement2.getId().toString(), measurement2));
    Assert.assertEquals(3, measurements.size());

    measurement = measurements.get("test_summary(statistic=count,x=a,y=b)");
    Assert.assertEquals(5000, measurement.getTimestamp(), 0);
    Assert.assertEquals(2, measurement.getValue(), 0);

    measurement = measurements.get("test_summary(statistic=total,x=a,y=b)");
    Assert.assertEquals(5000, measurement.getTimestamp(), 0);
    Assert.assertEquals(6, measurement.getValue(), 0);

    // 3 / 2000 * 1000
    measurement = measurements.get("test_summary(statistic=tps,x=a,y=b)");
    Assert.assertEquals(5000, measurement.getTimestamp(), 0);
    Assert.assertEquals(3, measurement.getValue(), 0);
  }
}
