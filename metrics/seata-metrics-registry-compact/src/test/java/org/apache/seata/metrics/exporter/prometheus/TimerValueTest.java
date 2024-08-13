package org.apache.seata.metrics.exporter.prometheus;


import org.apache.seata.metrics.registry.compact.TimerValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TimerValueTest {
    private TimerValue timerValue;

    @BeforeEach
    public void setUp() {
        timerValue = new TimerValue();
    }

    @Test
    public void testGetCount() {
        assertEquals(0, timerValue.getCount());
        timerValue.record(1, TimeUnit.SECONDS);
        assertEquals(1, timerValue.getCount());
    }

    @Test
    public void testGetTotal() {
        assertEquals(0, timerValue.getTotal());
        timerValue.record(1, TimeUnit.SECONDS);
        assertEquals(TimeUnit.MICROSECONDS.convert(1, TimeUnit.SECONDS), timerValue.getTotal());
    }

    @Test
    public void testGetMax() {
        assertEquals(0, timerValue.getMax());
        timerValue.record(1, TimeUnit.SECONDS);
        assertEquals(TimeUnit.MICROSECONDS.convert(1, TimeUnit.SECONDS), timerValue.getMax());
    }

    @Test
    public void testGetAverage() {
        assertEquals(0, timerValue.getAverage());
        timerValue.record(1, TimeUnit.SECONDS);
        assertEquals(1000000, timerValue.getAverage());
    }

    @Test
    public void testRecord() {
        timerValue.record(1, TimeUnit.SECONDS);
        assertEquals(1, timerValue.getCount());
        assertEquals(TimeUnit.MICROSECONDS.convert(1, TimeUnit.SECONDS), timerValue.getTotal());
        assertEquals(TimeUnit.MICROSECONDS.convert(1, TimeUnit.SECONDS), timerValue.getMax());
    }

    @Test
    public void testRecordNegativeValue() {
        timerValue.record(-1, TimeUnit.SECONDS);
        assertEquals(0, timerValue.getCount());
        assertEquals(0, timerValue.getTotal());
        assertEquals(0, timerValue.getMax());
    }
}
