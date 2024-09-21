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

import org.apache.seata.metrics.*;
import org.apache.seata.metrics.registry.compact.CompactRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CompactRegistryTest {
    @Mock
    private Id id;
    @Mock
    private Supplier<Number> supplier;

    private CompactRegistry compactRegistry;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        compactRegistry = new CompactRegistry();
    }

    @Test
    public void testGetGauge() {
        when(id.getName()).thenReturn("test");
        SortedMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("testTag", "testValue");
        when(id.getTags()).thenReturn(sortedMap.entrySet());
        when(id.getMeterKey()).thenReturn("testKey");
        when(supplier.get()).thenReturn(1);
        when(id.getMeterKey()).thenReturn("testKey");
        when(supplier.get()).thenReturn(1);

        Gauge<Number> gauge = compactRegistry.getGauge(id, supplier);

        assertEquals(id.getName(), gauge.getId().getName());
        assertEquals(id.getTags(), gauge.getId().getTags());
        assertEquals(1, gauge.get().intValue());
        compactRegistry.clearUp();
    }

    @Test
    public void testGetCounter() {
        when(id.getName()).thenReturn("test");
        SortedMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("testTag", "testValue");
        when(id.getTags()).thenReturn(sortedMap.entrySet());
        when(id.getMeterKey()).thenReturn("testKey");
        Counter counter = (Counter)compactRegistry.getCounter(id);

        Id id2 = new Id(id.getName()).withTag(id.getTags());
        assertEquals(id2.getName(), counter.getId().getName());
        assertEquals(id2.getTags(), counter.getId().getTags());
        compactRegistry.clearUp();
    }

    @Test
    public void testGetSummary() {
        when(id.getName()).thenReturn("test");
        SortedMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("testTag", "testValue");
        when(id.getTags()).thenReturn(sortedMap.entrySet());
        when(id.getMeterKey()).thenReturn("testKey");
        Summary summary = compactRegistry.getSummary(id);

        Id id2 = new Id(id.getName()).withTag(id.getTags());
        assertEquals(id2.getName(), summary.getId().getName());
        assertEquals(id2.getTags(), summary.getId().getTags());
        compactRegistry.clearUp();
    }

    @Test
    public void testGetTimer() {
        when(id.getName()).thenReturn("test");
        SortedMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("testTag", "testValue");
        when(id.getTags()).thenReturn(sortedMap.entrySet());
        when(id.getMeterKey()).thenReturn("testKey");
        Timer timer = compactRegistry.getTimer(id);

        Id id2 = new Id(id.getName()).withTag(id.getTags());
        assertEquals(id2.getName(), timer.getId().getName());
        assertEquals(id2.getTags(), timer.getId().getTags());
        compactRegistry.clearUp();
    }

}
