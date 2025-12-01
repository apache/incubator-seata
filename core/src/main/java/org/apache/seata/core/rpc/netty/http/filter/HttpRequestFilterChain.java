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

import org.apache.seata.core.exception.HttpRequestFilterException;

import java.util.List;
import java.util.function.Consumer;

public class HttpRequestFilterChain {
    private final List<HttpRequestFilter> filters;
    private final Consumer<HttpFilterContext<?>> finalAction;
    private int currentIndex = 0;

    public HttpRequestFilterChain(List<HttpRequestFilter> filters, Consumer<HttpFilterContext<?>> finalAction) {
        this.filters = filters;
        this.finalAction = finalAction;
    }

    public void doFilter(HttpFilterContext<?> httpFilterContext) throws HttpRequestFilterException {
        if (currentIndex < filters.size()) {
            HttpRequestFilter filter = filters.get(currentIndex++);
            filter.doFilter(httpFilterContext, this);
        } else {
            // Execute final action
            if (finalAction != null) {
                finalAction.accept(httpFilterContext);
            }
            // Reset for next request
            currentIndex = 0;
        }
    }

    /**
     * Get internal filter list (for unit test only).
     */
    List<HttpRequestFilter> getFilters() {
        return filters;
    }
}
