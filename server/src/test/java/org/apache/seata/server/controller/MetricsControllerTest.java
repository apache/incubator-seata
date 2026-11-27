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

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.common.rpc.http.HttpTextResponse;
import org.apache.seata.core.rpc.netty.http.SimpleHttp2Request;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.server.metrics.MetricsManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MetricsControllerTest {

    private final MetricsController controller = new MetricsController();
    private final MetricsManager manager = mock(MetricsManager.class);
    private MockedStatic<MetricsManager> managers;

    @BeforeEach
    void useTestManager() {
        managers = mockStatic(MetricsManager.class);
        managers.when(MetricsManager::get).thenReturn(manager);
    }

    @AfterEach
    void restoreManager() {
        managers.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getReturnsPrometheusText(boolean http2) throws IOException {
        Registry registry = mock(Registry.class);
        when(manager.getSharedPrometheusRegistry()).thenReturn(registry);
        Id id = new Id("seata.requests").withTag("role", "tc");
        when(registry.measure()).thenReturn(Collections.singletonList(new Measurement(id, 1234, 7)));

        HttpTextResponse response = controller.scrape(request(http2, HttpMethod.GET));

        assertEquals(200, response.getStatus());
        assertEquals(TextFormat.CONTENT_TYPE_004, response.getContentType());
        assertTrue(response.getBody().startsWith("# HELP seata seata\n"));
        assertTrue(Arrays.stream(response.getBody().split("\n"))
                .anyMatch(line -> line.startsWith("seata_requests{role=\"tc\"") && line.endsWith("} 7.0 1234")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void postAdvertisesGetWithoutCollectingMetrics(boolean http2) throws IOException {
        Registry registry = mock(Registry.class);
        when(manager.getSharedPrometheusRegistry()).thenReturn(registry);

        HttpTextResponse response = controller.scrape(request(http2, HttpMethod.POST));

        assertEquals(405, response.getStatus());
        assertEquals("GET", response.getHeaders().get("Allow"));
        assertEquals("", response.getBody());
        verifyNoInteractions(registry);
    }

    @Test
    void missingRegistryReturnsNotFound() throws IOException {
        HttpTextResponse response = controller.scrape(request(true, HttpMethod.GET));
        assertEquals(404, response.getStatus());
        assertEquals("", response.getBody());
    }

    private HttpContext<?> request(boolean http2, HttpMethod method) {
        if (http2) {
            SimpleHttp2Request request = new SimpleHttp2Request(method, "/metrics", new DefaultHttp2Headers(), "");
            return new HttpContext<>(request, null, true, HttpContext.HTTP_2_0);
        }
        return new HttpContext<>(new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, "/metrics"), null, true);
    }
}
