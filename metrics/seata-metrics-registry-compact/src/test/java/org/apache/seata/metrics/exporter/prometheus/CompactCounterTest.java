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

import org.apache.seata.metrics.Clock;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.registry.compact.CompactCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompactCounterTest {
    private CompactCounter compactCounter;
    private Id id;
    private Clock clock;

    @BeforeEach
    public void setup() {
        id = Mockito.mock(Id.class);
        clock = Mockito.mock(Clock.class);
        compactCounter = new CompactCounter(id, clock);
    }

    @Test
    public void testIncrease() {
        long value = 5L;
        long result = compactCounter.increase(value);
        assertEquals(value, result);
        assertEquals(value, compactCounter.get());
    }

    @Test
    public void testDecrease() {
        long value = 5L;
        compactCounter.increase(10L);
        long result = compactCounter.decrease(value);
        assertEquals(10L - value, result);
        assertEquals(10L - value, compactCounter.get());
    }

    @Test
    public void testGet() {
        long value = 10L;
        compactCounter.increase(value);
        assertEquals(value, compactCounter.get());
    }

    @Test
    public void testMeasure() {
        long value = 10L;
        compactCounter.increase(value);
        Mockito.when(clock.getCurrentMilliseconds()).thenReturn((double) 1000L);
        Measurement expectedMeasurement = new Measurement(id, 1000L, value);
        Iterable<Measurement> actualMeasurement = compactCounter.measure();
        Measurement next = actualMeasurement.iterator().next();

        assertEquals(expectedMeasurement.getId(), next.getId());
        assertEquals(expectedMeasurement.getTimestamp(), next.getTimestamp());
        assertEquals(expectedMeasurement.getValue(), next.getValue());
    }

    @Test
    public void testGetId() {
        assertEquals(id, compactCounter.getId());
    }
}