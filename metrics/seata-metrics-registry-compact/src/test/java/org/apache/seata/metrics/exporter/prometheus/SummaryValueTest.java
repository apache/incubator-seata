package org.apache.seata.metrics.exporter.prometheus;

import org.apache.seata.metrics.registry.compact.SummaryValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SummaryValueTest {

    private SummaryValue summaryValue;

    @BeforeEach
    public void setUp() {
        summaryValue = new SummaryValue(System.currentTimeMillis());
    }

    @Test
    public void testIncrease() {
        summaryValue.increase(5);
        assertEquals(5, summaryValue.getTotal());
        assertEquals(1, summaryValue.getCount());
    }

    @Test
    public void testIncreaseNoArgs() {
        SummaryValue summaryValue = new SummaryValue(System.currentTimeMillis());
        assertEquals(0, summaryValue.getCount());
        summaryValue.increase();
        assertEquals(1, summaryValue.getCount());
    }

    @Test
    public void testIncreaseNegative() {
        summaryValue.increase(-5);
        assertEquals(0, summaryValue.getTotal());
        assertEquals(0, summaryValue.getCount());
    }

    @Test
    public void testGetCount() {
        summaryValue.increase(5);
        summaryValue.increase(10);
        assertEquals(2, summaryValue.getCount());
    }

    @Test
    public void testGetTotal() {
        summaryValue.increase(5);
        summaryValue.increase(10);
        assertEquals(15, summaryValue.getTotal());
    }

    @Test
    public void testGetTps() {
        summaryValue.increase(5);
        summaryValue.increase(10);
        // Assuming that the time taken is 1 second
        double tps = summaryValue.getTps(System.currentTimeMillis() + 1000);
        assertEquals(15, tps, 10);
    }
}