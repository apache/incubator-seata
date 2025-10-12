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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@LoadLevel(name = "Http1", order = 1)
public class Http1HttpExecutor implements HttpExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http1HttpExecutor.class);

    private static final Map<Integer /*timeout*/, CloseableHttpClient> HTTP_CLIENT_MAP = new ConcurrentHashMap<>();

    private static final PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER =
            new PoolingHttpClientConnectionManager();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static volatile Http1HttpExecutor instance;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Http1HttpExecutor getInstance() {

        if (instance == null) {
            synchronized (Http1HttpExecutor.class) {
                if (instance == null) {
                    instance = new Http1HttpExecutor();
                }
            }
        }
        return instance;
    }

    static {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setMaxTotal(10);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(10);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> HTTP_CLIENT_MAP.values().parallelStream()
                .forEach(client -> {
                    try {
                        // delay 3s, make sure unregister http request send successfully
                        Thread.sleep(3000);
                        client.close();
                    } catch (IOException | InterruptedException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                })));
    }

    @Override
    public HttpResult doPost(String url, Map<String, String> params, Map<String, String> header, int timeout)
            throws IOException {
        try {
            URIBuilder builder = new URIBuilder(url);
            URI uri = builder.build();
            HttpPost httpPost = new HttpPost(uri);
            String contentType = "";
            if (header != null) {
                header.forEach(httpPost::addHeader);
                contentType = header.get("Content-Type");
            }
            if (StringUtils.isNotBlank(contentType)) {
                if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType)) {
                    List<NameValuePair> nameValuePairs = new ArrayList<>();
                    params.forEach((k, v) -> {
                        nameValuePairs.add(new BasicNameValuePair(k, v));
                    });
                    String requestBody = URLEncodedUtils.format(nameValuePairs, StandardCharsets.UTF_8);
                    StringEntity stringEntity = new StringEntity(requestBody, ContentType.APPLICATION_FORM_URLENCODED);
                    httpPost.setEntity(stringEntity);
                } else if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType)) {
                    String requestBody = OBJECT_MAPPER.writeValueAsString(params);
                    StringEntity stringEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(stringEntity);
                }
            }
            CloseableHttpClient client = getHttpClient(timeout);
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                int statusCode = response.getStatusLine() != null
                        ? response.getStatusLine().getStatusCode()
                        : 0;

                String responseBody = null;
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }

                return new HttpResult(statusCode, responseBody, null);
            }
        } catch (URISyntaxException | ClientProtocolException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public HttpResult doPost(String url, String body, Map<String, String> header, int timeout) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(url);
            URI uri = builder.build();
            HttpPost httpPost = new HttpPost(uri);
            String contentType = "";
            if (header != null) {
                header.forEach(httpPost::addHeader);
                contentType = header.get("Content-Type");
            }
            if (StringUtils.isNotBlank(contentType)) {
                if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType)) {
                    StringEntity stringEntity = new StringEntity(body, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(stringEntity);
                }
            }
            CloseableHttpClient client = getHttpClient(timeout);
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                int statusCode = response.getStatusLine() != null
                        ? response.getStatusLine().getStatusCode()
                        : 0;

                String responseBody = null;
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }

                return new HttpResult(statusCode, responseBody, null);
            }
        } catch (URISyntaxException | ClientProtocolException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public HttpResult doGet(String url, Map<String, String> param, Map<String, String> header, int timeout)
            throws IOException {
        try {
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, param.get(key));
                }
            }
            URI uri = builder.build();
            HttpGet httpGet = new HttpGet(uri);
            if (header != null) {
                header.forEach(httpGet::addHeader);
            }
            CloseableHttpClient client = getHttpClient(timeout);
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                int statusCode = response.getStatusLine() != null
                        ? response.getStatusLine().getStatusCode()
                        : 0;

                String responseBody = null;
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }

                return new HttpResult(statusCode, responseBody, null);
            }
        } catch (URISyntaxException | ClientProtocolException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public HttpResult doGet(String url, Map<String, String> headers, int timeout) throws IOException {
        // todo
        return null;
    }

    @Override
    public HttpResult doPostJson(String url, String jsonBody, Map<String, String> headers, int timeout)
            throws IOException {

        HttpPost post = new HttpPost(url);

        if (headers != null) {
            headers.forEach(post::addHeader);
        }
        post.setHeader("Content-Type", "application/json");

        if (StringUtils.isNotBlank(jsonBody)) {
            post.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }

        CloseableHttpClient client = getHttpClient(timeout);
        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode =
                    response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : 0;

            String responseBody = null;
            if (response.getEntity() != null) {
                responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }

            return new HttpResult(statusCode, responseBody, null);
        }
    }

    private static CloseableHttpClient getHttpClient(int timeout) {
        return HTTP_CLIENT_MAP.computeIfAbsent(timeout, k -> HttpClients.custom()
                .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(timeout)
                        .setSocketTimeout(timeout)
                        .setConnectTimeout(timeout)
                        .build())
                .build());
    }
}
