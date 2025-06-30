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

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.core.rpc.netty.http.filter.impl.XSSHttpRequestFilter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HttpRequestFilterManager {

    private static final List<HttpRequestFilter> HTTP_REQUEST_FILTERS = new ArrayList<>();
    private static final HttpRequestFilterChain HTTP_REQUEST_FILTER_CHAIN;

    static {
        boolean globalEnabled =
                ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_ENABLE, true);

        if (globalEnabled) {
            addIfEnabled(new XSSHttpRequestFilter());
        }

        HTTP_REQUEST_FILTERS.sort(Comparator.comparingInt(HttpRequestFilter::getOrder));
        HTTP_REQUEST_FILTER_CHAIN = new HttpRequestFilterChain(HTTP_REQUEST_FILTERS);
    }

    private static void addIfEnabled(HttpRequestFilter filter) {
        if (filter.shouldApply()) {
            HTTP_REQUEST_FILTERS.add(filter);
        }
    }

    public static HttpRequestFilterChain getFilterChain() {
        return HTTP_REQUEST_FILTER_CHAIN;
    }
}
