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
import org.apache.seata.metrics.registry.compact.CompactSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class CompactSummaryTest {
    @Mock
    private Id id;

    private CompactSummary compactSummary;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        compactSummary = new CompactSummary(id);
    }

    @Test
    public void testIncrease() {
        compactSummary.increase(5);
        assertEquals(5, compactSummary.total());
        assertEquals(1, compactSummary.count());
    }

    @Test
    public void testTotal() {
        compactSummary.increase(5);
        compactSummary.increase(10);
        assertEquals(15, compactSummary.total());
    }

    @Test
    public void testCount() {
        compactSummary.increase(5);
        compactSummary.increase(10);
        compactSummary.increase(15);
        assertEquals(3, compactSummary.count());
    }

    @Test
    public void testTps() {
        compactSummary.increase(5);
        compactSummary.increase(10);
        compactSummary.increase(15);
        try {
            //Avoid test failures that take less than 1 millisecond to complete
            Thread.sleep(1);
        } catch (InterruptedException ignore) {
            // don't care
        }
        // Assuming that the time taken is 1 second
        assertTrue(compactSummary.tps() >0);
    }

    @Test
    public void testMeasure() {
        compactSummary.increase(5);
        compactSummary.increase(10);
        compactSummary.increase(15);
        Iterable<Measurement> measurements = compactSummary.measure();
        Iterator<Measurement> iterator = measurements.iterator();
        assertEquals(3, iterator.next().getValue());
        assertEquals(30, iterator.next().getValue());
    }

    @Test
    public void testGetId() {
        when(id.getName()).thenReturn("test");
        when(id.getTags()).thenReturn(null);
        compactSummary = new CompactSummary(id);
        assertEquals("test", compactSummary.getId().getName());
    }
}
