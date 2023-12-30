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
package io.seata.spring.boot.autoconfigure.properties.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
public class MetricsPropertiesTest {

    @Test
    public void testMetricsProperties() {
        MetricsProperties metricsProperties = new MetricsProperties();
        metricsProperties.setRegistryType("type");
        metricsProperties.setExporterList("list");
        metricsProperties.setEnabled(true);
        metricsProperties.setExporterPrometheusPort(1);

        Assertions.assertEquals("type", metricsProperties.getRegistryType());
        Assertions.assertEquals("list", metricsProperties.getExporterList());
        Assertions.assertTrue(metricsProperties.getEnabled());
        Assertions.assertEquals(1, metricsProperties.getExporterPrometheusPort());
    }
}
