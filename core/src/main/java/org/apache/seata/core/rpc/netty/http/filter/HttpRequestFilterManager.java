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
package org.apache.seata.core.rpc.netty.http.filter;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HttpRequestFilterManager {

    private static final List<HttpRequestFilter> HTTP_REQUEST_FILTERS = new ArrayList<>();
    private static HttpRequestFilterChain HTTP_REQUEST_FILTER_CHAIN;

    private static volatile boolean initialized = false;

    public static synchronized void initializeFilters() {
        if (initialized) {
            return;
        }
        boolean enableFilter =
                ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_ENABLE, true);
        if (enableFilter) {
            List<HttpRequestFilter> httpRequestFilters = EnhancedServiceLoader.loadAll(HttpRequestFilter.class);
            for (HttpRequestFilter filter : httpRequestFilters) {
                if (filter.shouldApply()) {
                    HTTP_REQUEST_FILTERS.add(filter);
                }
            }

            HTTP_REQUEST_FILTERS.sort(Comparator.comparingInt(HttpRequestFilter::getOrder));
            HTTP_REQUEST_FILTER_CHAIN = new HttpRequestFilterChain(HTTP_REQUEST_FILTERS);
        }
        initialized = true;
    }

    public static HttpRequestFilterChain getFilterChain() {
        if (!initialized) {
            throw new IllegalStateException("HttpRequestFilterManager not initialized.");
        }
        return HTTP_REQUEST_FILTER_CHAIN;
    }
}
