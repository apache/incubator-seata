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

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.common.rpc.http.HttpTextResponse;
import org.apache.seata.core.rpc.netty.http.SimpleHttp2Request;
import org.apache.seata.metrics.exporter.prometheus.PrometheusExporter;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.server.metrics.MetricsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;

@RestController
public class MetricsController {

    @GetMapping("/metrics")
    public HttpTextResponse scrape(HttpContext<?> context) throws IOException {
        Registry registry = MetricsManager.get().getSharedPrometheusRegistry();
        if (registry == null) {
            return new HttpTextResponse(404, TextFormat.CONTENT_TYPE_004, "");
        }
        HttpMethod method = context.isHttp2()
                ? ((SimpleHttp2Request) context.getRequest()).getMethod()
                : ((HttpRequest) context.getRequest()).method();
        if (!HttpMethod.GET.equals(method)) {
            return new HttpTextResponse(405, TextFormat.CONTENT_TYPE_004, "", Collections.singletonMap("Allow", "GET"));
        }
        return new HttpTextResponse(200, TextFormat.CONTENT_TYPE_004, PrometheusExporter.scrape(registry));
    }
}
