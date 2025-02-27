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

import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.registry.compact.CompactTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CompactTimerTest {
    @Mock
    private Id id;

    private CompactTimer compactTimer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        compactTimer = new CompactTimer(id);
    }

    @Test
    public void testRecord() {
        compactTimer.record(5, TimeUnit.MILLISECONDS);
        assertEquals(1, compactTimer.count());
        assertEquals(5000, compactTimer.total());
        assertEquals(5000, compactTimer.max());
        assertEquals(5000, compactTimer.average(), 0.01);
    }

    @Test
    public void testCount() {
        compactTimer.record(5, TimeUnit.MILLISECONDS);
        compactTimer.record(10, TimeUnit.MILLISECONDS);
        assertEquals(2, compactTimer.count());
    }

    @Test
    public void testTotal() {
        compactTimer.record(5, TimeUnit.MILLISECONDS);
        compactTimer.record(10, TimeUnit.MILLISECONDS);
        assertEquals(15000, compactTimer.total());
    }

    @Test
    public void testMax() {
        compactTimer.record(5, TimeUnit.MILLISECONDS);
        compactTimer.record(10, TimeUnit.MILLISECONDS);
        assertEquals(10000, compactTimer.max());
    }

    @Test
    public void testAverage() {
        compactTimer.record(5, TimeUnit.MILLISECONDS);
        compactTimer.record(10, TimeUnit.MILLISECONDS);
        assertEquals(7500, compactTimer.average(), 0.01);
    }

    @Test
    public void testMeasure() {
        compactTimer.record(5, TimeUnit.MILLISECONDS);
        compactTimer.record(10, TimeUnit.MILLISECONDS);
        Iterable<Measurement> measurements = compactTimer.measure();
        Iterator<Measurement> iterator = measurements.iterator();
        assertEquals(2, iterator.next().getValue());
        assertEquals(15, iterator.next().getValue(), 0.01);
        assertEquals(10, iterator.next().getValue(), 0.01);
        assertEquals(7.5, iterator.next().getValue(), 0.01);
    }

    @Test
    public void testGetId() {
        when(id.getName()).thenReturn("test");
        when(id.getTags()).thenReturn(null);
        compactTimer = new CompactTimer(id);
        assertEquals("test", compactTimer.getId().getName());
    }
}
