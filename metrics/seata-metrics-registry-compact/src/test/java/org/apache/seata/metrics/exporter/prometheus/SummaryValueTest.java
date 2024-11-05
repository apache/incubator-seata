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

import org.apache.seata.metrics.registry.compact.SummaryValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(tps > 0);
    }
}