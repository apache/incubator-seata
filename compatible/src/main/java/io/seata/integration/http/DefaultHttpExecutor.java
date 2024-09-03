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

/**
 * The type Default http executor.
 */
@Deprecated
public class DefaultHttpExecutor {

    private static final org.apache.seata.integration.http.DefaultHttpExecutor INSTANCE
        = org.apache.seata.integration.http.DefaultHttpExecutor.getInstance();

    private final org.apache.seata.integration.http.DefaultHttpExecutor targetDefaultHttpExecutor;

    private DefaultHttpExecutor(final org.apache.seata.integration.http.DefaultHttpExecutor innerInstance) {
        this.targetDefaultHttpExecutor = innerInstance;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static DefaultHttpExecutor getInstance() {
        return new DefaultHttpExecutor(INSTANCE);
    }

    /**
     * Build client entity.
     *
     * @param <T>         the type parameter
     * @param httpClient  the http client
     * @param paramObject the param object
     */
    public <T> void buildClientEntity(CloseableHttpClient httpClient, T paramObject) {
        this.targetDefaultHttpExecutor.buildClientEntity(httpClient, paramObject);
    }

    /**
     * Build get headers.
     *
     * @param <T>         the type parameter
     * @param headers     the headers
     * @param paramObject the param object
     */
    public <T> void buildGetHeaders(Map<String, String> headers, T paramObject) {
        this.targetDefaultHttpExecutor.buildGetHeaders(headers, paramObject);
    }

    /**
     * Init get url string.
     *
     * @param host   the host
     * @param path   the path
     * @param querys the querys
     * @return the string
     */
    public String initGetUrl(String host, String path, Map<String, String> querys) {
        return this.targetDefaultHttpExecutor.initGetUrl(host, path, querys);
    }

    /**
     * Build post headers.
     *
     * @param <T>     the type parameter
     * @param headers the headers
     * @param t       the t
     */
    public <T> void buildPostHeaders(Map<String, String> headers, T t) {
        this.targetDefaultHttpExecutor.buildPostHeaders(headers, t);
    }

    /**
     * Build entity string entity.
     *
     * @param <T>    the type parameter
     * @param entity the entity
     * @param t      the t
     * @return the string entity
     */
    public <T> StringEntity buildEntity(StringEntity entity, T t) {
        return this.targetDefaultHttpExecutor.buildEntity(entity, t);
    }

    /**
     * Convert result k.
     *
     * @param <K>      the type parameter
     * @param response the response
     * @param clazz    the clazz
     * @return the k
     */
    public <K> K convertResult(HttpResponse response, Class<K> clazz) {
        return this.targetDefaultHttpExecutor.convertResult(response, clazz);
    }

}
