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
package io.seata.integration.http;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

public class DefaultHttpExecutor {

    private static final org.apache.seata.integration.http.DefaultHttpExecutor INSTANCE
        = org.apache.seata.integration.http.DefaultHttpExecutor.getInstance();

    private final org.apache.seata.integration.http.DefaultHttpExecutor targetDefaultHttpExecutor;

    private DefaultHttpExecutor(final org.apache.seata.integration.http.DefaultHttpExecutor innerInstance) {
        this.targetDefaultHttpExecutor = innerInstance;
    }

    public static DefaultHttpExecutor getInstance() {
        return new DefaultHttpExecutor(INSTANCE);
    }

    public <T> void buildClientEntity(CloseableHttpClient httpClient, T paramObject) {
        this.targetDefaultHttpExecutor.buildClientEntity(httpClient, paramObject);
    }

    public <T> void buildGetHeaders(Map<String, String> headers, T paramObject) {
        this.targetDefaultHttpExecutor.buildGetHeaders(headers, paramObject);
    }

    public String initGetUrl(String host, String path, Map<String, String> querys) {
        return this.targetDefaultHttpExecutor.initGetUrl(host, path, querys);
    }

    public <T> void buildPostHeaders(Map<String, String> headers, T t) {
        this.targetDefaultHttpExecutor.buildPostHeaders(headers, t);
    }

    public <T> StringEntity buildEntity(StringEntity entity, T t) {
        return this.targetDefaultHttpExecutor.buildEntity(entity, t);
    }

    public <K> K convertResult(HttpResponse response, Class<K> clazz) {
        return this.targetDefaultHttpExecutor.convertResult(response, clazz);
    }

}
