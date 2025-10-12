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
package org.apache.seata.common.http;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpExecutorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpExecutorFactory.class);

    private static final String HTTP1_IMPL = "Http1";
    private static final String HTTP2_IMPL = "Http2";
    private static final HttpExecutor INSTANCE = createInstance();

    private HttpExecutorFactory() {}

    private static HttpExecutor createInstance() {
        String implName = isOkHttpAvailable() ? HTTP1_IMPL : HTTP2_IMPL;
        return EnhancedServiceLoader.load(HttpExecutor.class, implName);
    }

    private static boolean isOkHttpAvailable() {
        try {
            Class.forName("okhttp3.OkHttpClient", false, HttpExecutorFactory.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            LOGGER.warn("HTTP2 implementation not available in classpath, falling back to default executor!");
            return false;
        }
    }

    public static HttpExecutor getInstance() {
        return INSTANCE;
    }
}
