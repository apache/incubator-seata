/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
