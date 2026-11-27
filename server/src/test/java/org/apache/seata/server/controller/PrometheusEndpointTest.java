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
package org.apache.seata.server.controller;

import io.prometheus.client.exporter.common.TextFormat;
import okhttp3.Response;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.metrics.Id;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.metrics.MetricsManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusEndpointTest extends BaseSpringBootTest {

    @Test
    void scrapesRegisteredMetricsOnTcServicePort() throws IOException {
        MetricsManager.get().getRegistry().getGauge(new Id("seata.endpoint.test").withTag("component", "tc"), () -> 7);

        String url = "http://127.0.0.1:" + XID.getPort() + "/metrics";
        try (Response response = HttpClientUtil.doPost(url, "", null, 5000)) {
            assertEquals(405, response.code());
            assertEquals("GET", response.header("Allow"));
        }
        try (Response response = HttpClientUtil.doGet(url, null, null, 5000)) {
            assertEquals(200, response.code());
            assertEquals(TextFormat.CONTENT_TYPE_004, response.header("Content-Type"));
            String body = response.body().string();
            assertTrue(
                    body.lines()
                            .anyMatch(line ->
                                    line.startsWith("seata_endpoint_test{component=\"tc\"") && line.contains("} 7.0 ")),
                    body);
        }
    }
}
