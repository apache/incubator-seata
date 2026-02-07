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
package org.apache.seata.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HttpClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final Map<Integer /*timeout*/, OkHttpClient> HTTP_CLIENT_MAP = new ConcurrentHashMap<>();

    private static final Map<Integer /*readTimeoutSeconds*/, OkHttpClient> HTTP2_CLIENT_MAP = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    public static final MediaType MEDIA_TYPE_FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded");

    /**
     * Fixed connection timeout for HTTP/2 watch connections (in seconds).
     * Set to 10 seconds for fast failure when the server is unreachable.
     */
    private static final int HTTP2_WATCH_CONNECT_TIMEOUT_SECONDS = 10;

    /**
     * Default read timeout for HTTP/2 watch connections (in seconds).
     * Used when no custom read timeout is specified. A finite value avoids indefinite blocking
     * when the server dies without closing the TCP connection (e.g. crash). Clients can reconnect
     * after timeout. Use overloaded watch methods with readTimeoutSeconds to customize.
     */
    private static final int HTTP2_WATCH_READ_TIMEOUT_SECONDS_DEFAULT = 300;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            HTTP_CLIENT_MAP.values().parallelStream().forEach(client -> {
                try {
                    // Delay 3 seconds to ensure unregister HTTP requests are sent successfully
                    Thread.sleep(3000);
                    client.dispatcher().executorService().shutdown();
                    // Wait for up to 3 seconds for in-flight requests to complete
                    if (!client.dispatcher().executorService().awaitTermination(3, TimeUnit.SECONDS)) {
                        LOGGER.warn("Timeout waiting for OkHttp executor service to terminate.");
                    }
                    client.connectionPool().evictAll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted while waiting for OkHttp executor service to terminate.", e);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            });

            HTTP2_CLIENT_MAP.values().parallelStream().forEach(client -> {
                try {
                    client.dispatcher().executorService().shutdown();
                    // Wait for up to 3 seconds for in-flight requests to complete
                    if (!client.dispatcher().executorService().awaitTermination(3, TimeUnit.SECONDS)) {
                        LOGGER.warn("Timeout waiting for OkHttp executor service to terminate.");
                    }
                    client.connectionPool().evictAll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted while waiting for OkHttp executor service to terminate.", e);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            });
        }));
    }

    public static Response doPost(String url, Map<String, String> params, Map<String, String> header, int timeout)
            throws IOException {
        String contentType = header != null ? header.get("Content-Type") : "";
        RequestBody requestBody = createRequestBody(params, contentType);
        Request request = buildRequest(url, header, requestBody, "POST");
        OkHttpClient client = createHttp1ClientWithTimeout(timeout);
        return client.newCall(request).execute();
    }

    public static Response doPost(String url, String body, Map<String, String> header, int timeout) throws IOException {
        String contentType = header != null ? header.get("Content-Type") : "";
        MediaType mediaType = StringUtils.isNotBlank(contentType) ? MediaType.parse(contentType) : MEDIA_TYPE_JSON;
        RequestBody requestBody = StringUtils.isNotBlank(body)
                ? RequestBody.create(body, mediaType)
                : RequestBody.create(new byte[0], mediaType);
        Request request = buildRequest(url, header, requestBody, "POST");
        OkHttpClient client = createHttp1ClientWithTimeout(timeout);
        return client.newCall(request).execute();
    }

    public static Response doGet(String url, Map<String, String> param, Map<String, String> header, int timeout)
            throws IOException {
        String urlWithParams = buildUrlWithParams(url, param);
        Request request = buildRequest(urlWithParams, header, null, "GET");
        OkHttpClient client = createHttp1ClientWithTimeout(timeout);
        return client.newCall(request).execute();
    }

    public static Response doPostJson(String url, String jsonBody, Map<String, String> headers, int timeout)
            throws IOException {
        RequestBody requestBody = jsonBody != null
                ? RequestBody.create(jsonBody, MEDIA_TYPE_JSON)
                : RequestBody.create(new byte[0], MEDIA_TYPE_JSON);
        Map<String, String> headersWithContentType =
                headers != null ? new java.util.HashMap<>(headers) : new java.util.HashMap<>();
        headersWithContentType.put("Content-Type", "application/json");
        Request request = buildRequest(url, headersWithContentType, requestBody, "POST");
        OkHttpClient client = createHttp1ClientWithTimeout(timeout);
        return client.newCall(request).execute();
    }

    private static RequestBody createRequestBody(Map<String, String> params, String contentType)
            throws JsonProcessingException {
        if (params == null || params.isEmpty()) {
            return RequestBody.create(new byte[0]);
        }

        // Extract media type without parameters for robust comparison
        String mediaTypeOnly = contentType == null ? "" : contentType.split(";")[0].trim();
        if (MEDIA_TYPE_FORM_URLENCODED.toString().equals(mediaTypeOnly)) {
            FormBody.Builder formBuilder = new FormBody.Builder();
            params.forEach(formBuilder::add);
            return formBuilder.build();
        } else {
            String json = OBJECT_MAPPER.writeValueAsString(params);
            return RequestBody.create(json, MEDIA_TYPE_JSON);
        }
    }

    private static OkHttpClient createHttp1ClientWithTimeout(int timeoutMillis) {
        return HTTP_CLIENT_MAP.computeIfAbsent(timeoutMillis, k -> new OkHttpClient.Builder()
                // Use HTTP/1.1 (default protocol, no need to specify)
                .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .build());
    }

    private static Request buildRequest(
            String url, Map<String, String> headers, RequestBody requestBody, String method) {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers != null) {
            headers.forEach(headerBuilder::add);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url).headers(headerBuilder.build());

        if ("POST".equals(method)) {
            if (requestBody == null) {
                requestBody = RequestBody.create(new byte[0], MEDIA_TYPE_JSON);
            }
            requestBuilder.post(requestBody);
        } else if ("GET".equals(method)) {
            requestBuilder.get();
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        return requestBuilder.build();
    }

    private static String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        StringBuilder urlBuilder = new StringBuilder(url);
        boolean first = !url.contains("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                urlBuilder.append("?");
                first = false;
            } else {
                urlBuilder.append("&");
            }
            try {
                urlBuilder
                        .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            } catch (java.io.UnsupportedEncodingException e) {
                // UTF-8 is always supported
                throw new RuntimeException(e);
            }
        }
        return urlBuilder.toString();
    }

    /**
     * Create an HTTP/2 client for watch connections.
     * This client is configured for long-lived connections to receive Server-Sent Events (SSE).
     * The client instances are cached and reused by read timeout (key) to improve performance.
     * Connect timeout is fixed at 10 seconds.
     *
     * @param readTimeoutSeconds read timeout in seconds (0 = infinite; finite value avoids indefinite block on server crash)
     * @return configured OkHttpClient instance (cached and reused)
     */
    private static OkHttpClient createHttp2WatchClient(int readTimeoutSeconds) {
        return HTTP2_CLIENT_MAP.computeIfAbsent(readTimeoutSeconds, k -> new OkHttpClient.Builder()
                .protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE))
                .connectTimeout(HTTP2_WATCH_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(HTTP2_WATCH_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build());
    }

    public static <T> SeataHttpWatch<T> watch(String url, Map<String, String> headers, Class<T> eventType)
            throws IOException {
        return watch(url, headers, null, "GET", eventType, HTTP2_WATCH_READ_TIMEOUT_SECONDS_DEFAULT);
    }

    public static <T> SeataHttpWatch<T> watch(String url, Class<T> eventType) throws IOException {
        return watch(url, null, null, "GET", eventType, HTTP2_WATCH_READ_TIMEOUT_SECONDS_DEFAULT);
    }

    /**
     * Execute a watch request with specified HTTP method and read timeout.
     * This method creates a long-lived HTTP/2 connection to receive Server-Sent Events (SSE).
     *
     * @param url the URL to watch (must not be null or blank)
     * @param headers HTTP headers
     * @param requestBody request body (optional)
     * @param method HTTP method (GET, POST, PUT)
     * @param eventType the class type for deserializing event data
     * @param readTimeoutSeconds read timeout in seconds (0 = infinite)
     * @param <T> the event data type
     * @return a Watch instance for receiving SSE events
     * @throws IOException if the request fails
     * @throws IllegalArgumentException if the URL is null or blank
     */
    private static <T> SeataHttpWatch<T> watch(
            String url,
            Map<String, String> headers,
            RequestBody requestBody,
            String method,
            Class<T> eventType,
            int readTimeoutSeconds)
            throws IOException {

        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }

        OkHttpClient client = createHttp2WatchClient(readTimeoutSeconds);
        Request request = buildHttp2WatchRequest(url, headers, requestBody, method);
        return SeataHttpWatch.createWatch(client, request, eventType);
    }

    public static <T> SeataHttpWatch<T> watch(
            String url, Map<String, String> headers, Class<T> eventType, int readTimeoutSeconds) throws IOException {
        return watch(url, headers, null, "GET", eventType, readTimeoutSeconds);
    }

    public static <T> SeataHttpWatch<T> watch(String url, Class<T> eventType, int readTimeoutSeconds)
            throws IOException {
        return watch(url, null, null, "GET", eventType, readTimeoutSeconds);
    }

    public static <T> SeataHttpWatch<T> watchPost(
            String url, Map<String, String> params, Map<String, String> headers, Class<T> eventType)
            throws IOException {
        try {
            String contentType = headers != null ? headers.get("Content-Type") : "";
            RequestBody requestBody = createRequestBody(params, contentType);
            return watch(url, headers, requestBody, "POST", eventType, HTTP2_WATCH_READ_TIMEOUT_SECONDS_DEFAULT);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to create request body", e);
            throw new IOException("Failed to create request body", e);
        }
    }

    public static <T> SeataHttpWatch<T> watchPost(
            String url,
            Map<String, String> params,
            Map<String, String> headers,
            Class<T> eventType,
            int readTimeoutSeconds)
            throws IOException {
        try {
            String contentType = headers != null ? headers.get("Content-Type") : "";
            RequestBody requestBody = createRequestBody(params, contentType);
            return watch(url, headers, requestBody, "POST", eventType, readTimeoutSeconds);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to create request body", e);
            throw new IOException("Failed to create request body", e);
        }
    }

    public static <T> SeataHttpWatch<T> watchPost(String url, Map<String, String> params, Class<T> eventType)
            throws IOException {
        return watchPost(url, params, null, eventType);
    }

    public static <T> SeataHttpWatch<T> watchPost(
            String url, Map<String, String> params, Class<T> eventType, int readTimeoutSeconds) throws IOException {
        return watchPost(url, params, null, eventType, readTimeoutSeconds);
    }

    private static Request buildHttp2WatchRequest(
            String url, Map<String, String> headers, RequestBody requestBody, String method) {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers != null) {
            headers.forEach(headerBuilder::add);
        }
        // Always add Accept header for SSE
        headerBuilder.add("Accept", "text/event-stream");

        Request.Builder requestBuilder = new Request.Builder().url(url).headers(headerBuilder.build());

        if ("POST".equals(method) && requestBody != null) {
            requestBuilder.post(requestBody);
        } else if ("PUT".equals(method) && requestBody != null) {
            requestBuilder.put(requestBody);
        } else if ("GET".equals(method)) {
            requestBuilder.get();
        } else {
            // Default to GET if method is not specified or not supported
            requestBuilder.get();
        }

        return requestBuilder.build();
    }
}
