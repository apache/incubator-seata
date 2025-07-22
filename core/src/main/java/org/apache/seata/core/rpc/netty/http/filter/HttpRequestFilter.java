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

/**
 * Interface for Netty HTTP request filters with order and enable control.
 */
public interface HttpRequestFilter {

    /**
     * Filter execution order; lower values run first.
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Executes the filter logic.
     */
    void doFilter(HttpFilterContext<?> context) throws HttpRequestFilterException;

    /**
     * Determines if the filter should run.
     */
    boolean shouldApply();
}
