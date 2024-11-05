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
import org.apache.seata.metrics.registry.compact.CompactGauge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CompactGaugeTest {
    @Mock
    private Id id;
    @Mock
    private Supplier<Number> supplier;
    @Mock
    private Clock clock;

    private CompactGauge<Number> compactGauge;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        compactGauge = new CompactGauge<>(id, supplier, clock);
    }

    @Test
    public void testGet() {
        Number expected = 10;
        when(supplier.get()).thenReturn(expected);

        Number actual = compactGauge.get();

        assertEquals(expected, actual);
        verify(supplier, times(1)).get();
    }

    @Test
    public void testGetId() {
        assertEquals(id, compactGauge.getId());
    }

    @Test
    public void testMeasure() {
        Number expectedValue = 10;
        when(supplier.get()).thenReturn(expectedValue);
        double expectedTimestamp = 1000.0;
        when(clock.getCurrentMilliseconds()).thenReturn(expectedTimestamp);

        Measurement actualMeasurement = compactGauge.measure().iterator().next();

        assertEquals(id, actualMeasurement.getId());
        assertEquals(expectedTimestamp, actualMeasurement.getTimestamp());
        assertEquals(expectedValue.doubleValue(), actualMeasurement.getValue());
    }
}
