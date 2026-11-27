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
import org.apache.seata.metrics.registry.Registry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.apache.seata.common.DefaultValues.DEFAULT_PROMETHEUS_PORT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test {@link PrometheusExporter}
 *
 */
public class PrometheusExporterTest {

    @Test
    public void getGetUnknownType() {
        Assertions.assertDoesNotThrow(() -> {
            PrometheusExporter.getUnknownType();
        });
    }

    @Test
    public void scrapesRegisteredMeasurementsOnTheTcPortByDefault() throws IOException {
        try (PrometheusExporter exporter = new PrometheusExporter(DEFAULT_PROMETHEUS_PORT)) {
            Assertions.assertTrue(exporter.usesTcPort());
            Assertions.assertEquals("", PrometheusExporter.scrape(null));

            Registry registry = mock(Registry.class);
            Id id = new Id("seata.transaction").withTag("app", "orders");
            when(registry.measure()).thenReturn(Collections.singletonList(new Measurement(id, 1234, 7)));

            String text = PrometheusExporter.scrape(registry);
            Assertions.assertTrue(text.contains("# HELP seata seata\n"));
            Assertions.assertTrue(text.contains("seata_transaction{app=\"orders\""));
            Assertions.assertTrue(text.contains("} 7.0 1234\n"));
        }
    }

    @Test
    public void zeroStillStartsADedicatedListener() throws IOException {
        try (PrometheusExporter exporter = new PrometheusExporter(0)) {
            Assertions.assertFalse(exporter.usesTcPort());
        }
    }

    @Test
    public void rejectsPortsBelowTheTcPortSentinel() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PrometheusExporter(-2));
    }
}
