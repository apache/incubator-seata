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
package org.apache.seata.core.rpc.netty.http.filter.impl;

import io.netty.handler.codec.http.HttpRequest;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.core.rpc.netty.http.filter.FilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;

import java.util.List;
import java.util.Map;

public class XssHttpRequestFilter implements HttpRequestFilter {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void filter(HttpRequest request, HttpRequestParamWrapper paramWrapper) throws FilterException {
        Map<String, List<String>> allParams = paramWrapper.getAllParamsAsMultiMap();
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            for (String value : entry.getValue()) {
                if (value != null && containsXssRisk(value)) {
                    throw new FilterException("XSS risk detected in param: " + entry.getKey() + ", value: " + value);
                }
            }
        }
    }

    @Override
    public boolean shouldFilter() {
        return ConfigurationFactory.getInstance()
                .getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_XSS_ENABLED, true);
    }

    private boolean containsXssRisk(String value) {
        String lower = value.toLowerCase();
        return lower.contains("<script>") || lower.contains("</script>") || lower.contains("onerror") || lower.contains("onload");
    }
}
