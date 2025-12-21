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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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
import org.apache.seata.common.executor.HttpCallback;
import org.apache.seata.common.executor.StreamHttpCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import okio.BufferedSource;

public class HttpClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final Map<Integer /*timeout*/, CloseableHttpClient> HTTP_CLIENT_MAP = new ConcurrentHashMap<>();

    private static final PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER =
            new PoolingHttpClientConnectionManager();

    private static final Map<Integer /*timeout*/, OkHttpClient> HTTP2_CLIENT_MAP = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    public static final MediaType MEDIA_TYPE_FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded");

    static {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setMaxTotal(10);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(10);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            HTTP_CLIENT_MAP.values().parallelStream().forEach(client -> {
                try {
                    // delay 3s, make sure unregister http request send successfully
                    Thread.sleep(3000);
                    client.close();
                } catch (IOException | InterruptedException e) {
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

    // post request
    public static CloseableHttpResponse doPost(
            String url, Map<String, String> params, Map<String, String> header, int timeout) throws IOException {
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
            CloseableHttpClient client = HTTP_CLIENT_MAP.computeIfAbsent(timeout, k -> HttpClients.custom()
                    .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectionRequestTimeout(timeout)
                            .setSocketTimeout(timeout)
                            .setConnectTimeout(timeout)
                            .build())
                    .build());
            return client.execute(httpPost);
        } catch (URISyntaxException | ClientProtocolException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    // post request
    public static CloseableHttpResponse doPost(String url, String body, Map<String, String> header, int timeout)
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
                if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType)) {
                    StringEntity stringEntity = new StringEntity(body, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(stringEntity);
                }
            }
            CloseableHttpClient client = HTTP_CLIENT_MAP.computeIfAbsent(timeout, k -> HttpClients.custom()
                    .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectionRequestTimeout(timeout)
                            .setSocketTimeout(timeout)
                            .setConnectTimeout(timeout)
                            .build())
                    .build());
            return client.execute(httpPost);
        } catch (URISyntaxException | ClientProtocolException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    // get request
    public static CloseableHttpResponse doGet(
            String url, Map<String, String> param, Map<String, String> header, int timeout) throws IOException {
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
            CloseableHttpClient client = HTTP_CLIENT_MAP.computeIfAbsent(timeout, k -> HttpClients.custom()
                    .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectionRequestTimeout(timeout)
                            .setSocketTimeout(timeout)
                            .setConnectTimeout(timeout)
                            .build())
                    .build());
            return client.execute(httpGet);
        } catch (URISyntaxException | ClientProtocolException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static CloseableHttpResponse doPostJson(
            String url, String jsonBody, Map<String, String> headers, int timeout) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .build();

        HttpPost post = new HttpPost(url);
        post.setConfig(requestConfig);

        if (headers != null) {
            headers.forEach(post::addHeader);
        }
        post.setHeader("Content-Type", "application/json");

        StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
        post.setEntity(entity);

        CloseableHttpClient client = HttpClients.createDefault();
        return client.execute(post);
    }

    public static void doPostWithHttp2(
            String url, Map<String, String> params, Map<String, String> headers, HttpCallback<Response> callback) {
        doPostWithHttp2(url, params, headers, callback, 10);
    }

    public static void doPostWithHttp2(
            String url,
            Map<String, String> params,
            Map<String, String> headers,
            HttpCallback<Response> callback,
            int timeoutSeconds) {
        try {
            String contentType = headers != null ? headers.get("Content-Type") : "";
            RequestBody requestBody = createRequestBody(params, contentType);
            Request request = buildHttp2Request(url, headers, requestBody, "POST");
            OkHttpClient client = createHttp2ClientWithTimeout(timeoutSeconds);
            executeAsync(client, request, callback);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            callback.onFailure(e);
        }
    }

    public static void doPostWithHttp2(
            String url, String body, Map<String, String> headers, HttpCallback<Response> callback) {
        // default timeout 10 seconds
        doPostWithHttp2(url, body, headers, callback, 10);
    }

    public static void doPostWithHttp2(
            String url, String body, Map<String, String> headers, HttpCallback<Response> callback, int timeoutSeconds) {
        RequestBody requestBody = RequestBody.create(body, MEDIA_TYPE_JSON);
        Request request = buildHttp2Request(url, headers, requestBody, "POST");
        OkHttpClient client = createHttp2ClientWithTimeout(timeoutSeconds);
        executeAsync(client, request, callback);
    }

    public static void doGetWithHttp2(
            String url, Map<String, String> headers, final HttpCallback<Response> callback, int timeoutSeconds) {
        Request request = buildHttp2Request(url, headers, null, "GET");
        OkHttpClient client = createHttp2ClientWithTimeout(timeoutSeconds);
        executeAsync(client, request, callback);
    }

    /**
     * Execute a streaming POST request with HTTP/2 support.
     * This method allows processing data chunks as they arrive from the server.
     *
     * @param url the request URL
     * @param params the request parameters
     * @param headers the request headers
     * @param streamCallback the stream callback for handling data chunks
     * @param timeoutSeconds the timeout in seconds
     */
    public static void doPostStreamWithHttp2(
            String url,
            Map<String, String> params,
            Map<String, String> headers,
            StreamHttpCallback streamCallback,
            int timeoutSeconds) {
        try {
            String contentType = headers != null ? headers.get("Content-Type") : "";
            RequestBody requestBody = createRequestBody(params, contentType);
            Request request = buildHttp2Request(url, headers, requestBody, "POST");
            OkHttpClient client = createHttp2ClientWithTimeout(timeoutSeconds);
            executeStreamAsync(client, request, streamCallback);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            streamCallback.onError(e);
        }
    }

    /**
     * Execute a streaming POST request with HTTP/2 support (default timeout 10 seconds).
     *
     * @param url the request URL
     * @param params the request parameters
     * @param headers the request headers
     * @param streamCallback the stream callback for handling data chunks
     */
    public static void doPostStreamWithHttp2(
            String url,
            Map<String, String> params,
            Map<String, String> headers,
            StreamHttpCallback streamCallback) {
        doPostStreamWithHttp2(url, params, headers, streamCallback, 10);
    }

    /**
     * Execute a streaming POST request with HTTP/2 support using a JSON body.
     *
     * @param url the request URL
     * @param body the JSON body string
     * @param headers the request headers
     * @param streamCallback the stream callback for handling data chunks
     * @param timeoutSeconds the timeout in seconds
     */
    public static void doPostStreamWithHttp2(
            String url,
            String body,
            Map<String, String> headers,
            StreamHttpCallback streamCallback,
            int timeoutSeconds) {
        RequestBody requestBody = RequestBody.create(body, MEDIA_TYPE_JSON);
        Request request = buildHttp2Request(url, headers, requestBody, "POST");
        OkHttpClient client = createHttp2ClientWithTimeout(timeoutSeconds);
        executeStreamAsync(client, request, streamCallback);
    }

    /**
     * Execute a streaming POST request with HTTP/2 support using a JSON body (default timeout 10 seconds).
     *
     * @param url the request URL
     * @param body the JSON body string
     * @param headers the request headers
     * @param streamCallback the stream callback for handling data chunks
     */
    public static void doPostStreamWithHttp2(
            String url,
            String body,
            Map<String, String> headers,
            StreamHttpCallback streamCallback) {
        doPostStreamWithHttp2(url, body, headers, streamCallback, 10);
    }

    /**
     * Execute a streaming GET request with HTTP/2 support.
     * This method allows processing data chunks as they arrive from the server.
     *
     * @param url the request URL
     * @param headers the request headers
     * @param streamCallback the stream callback for handling data chunks
     * @param timeoutSeconds the timeout in seconds
     */
    public static void doGetStreamWithHttp2(
            String url,
            Map<String, String> headers,
            StreamHttpCallback streamCallback,
            int timeoutSeconds) {
        Request request = buildHttp2Request(url, headers, null, "GET");
        OkHttpClient client = createHttp2ClientWithTimeout(timeoutSeconds);
        executeStreamAsync(client, request, streamCallback);
    }

    /**
     * Execute a streaming GET request with HTTP/2 support (default timeout 10 seconds).
     *
     * @param url the request URL
     * @param headers the request headers
     * @param streamCallback the stream callback for handling data chunks
     */
    public static void doGetStreamWithHttp2(
            String url,
            Map<String, String> headers,
            StreamHttpCallback streamCallback) {
        doGetStreamWithHttp2(url, headers, streamCallback, 10);
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

    private static OkHttpClient createHttp2ClientWithTimeout(int timeoutSeconds) {
        return HTTP2_CLIENT_MAP.computeIfAbsent(timeoutSeconds, k -> new OkHttpClient.Builder()
                // Use HTTP/2 prior knowledge to directly use HTTP/2 without an initial HTTP/1.1 upgrade
                .protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE))
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build());
    }

    private static Request buildHttp2Request(
            String url, Map<String, String> headers, RequestBody requestBody, String method) {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers != null) {
            headers.forEach(headerBuilder::add);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url).headers(headerBuilder.build());

        if ("POST".equals(method) && requestBody != null) {
            requestBuilder.post(requestBody);
        } else if ("GET".equals(method)) {
            requestBuilder.get();
        }

        return requestBuilder.build();
    }

    private static void executeAsync(OkHttpClient client, Request request, final HttpCallback<Response> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                try {
                    callback.onSuccess(response);
                } finally {
                    response.close();
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    callback.onCancelled();
                } else {
                    callback.onFailure(e);
                }
            }
        });
    }

    /**
     * Execute an asynchronous streaming request.
     * This method reads data chunks from the response body and calls the stream callback
     * as data arrives, enabling real-time processing of server-pushed data.
     *
     * @param client the OkHttpClient instance
     * @param request the HTTP request
     * @param streamCallback the stream callback for handling data chunks
     */
    private static void executeStreamAsync(
            OkHttpClient client,
            Request request,
            final StreamHttpCallback streamCallback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    // Check if request was cancelled before processing
                    if (call.isCanceled()) {
                        streamCallback.onCancelled();
                        return;
                    }

                    // Notify that headers have been received
                    streamCallback.onHeaders(response);

                    // Check response status
                    if (!response.isSuccessful()) {
                        streamCallback.onError(new IOException("Unexpected response code: " + response.code()));
                        return;
                    }

                    // Get the response body source for streaming
                    if (response.body() == null) {
                        streamCallback.onComplete();
                        return;
                    }

                    BufferedSource source = response.body().source();

                    // Set buffer size for reading data chunks (8KB default)
                    int bufferSize = 8192;

                    // Read data chunks continuously until stream is closed (endStream=true)
                    // For HTTP/2 streaming, we need to keep reading until the server sends endStream=true
                    // The blocking read will wait for data to arrive, allowing real-time processing
                    while (true) {
                        // Check if request was cancelled during processing
                        if (call.isCanceled()) {
                            streamCallback.onCancelled();
                            return;
                        }

                        // Blocking read: waits for data to arrive
                        // Returns -1 when stream is closed (endStream=true received)
                        // Returns 0 or positive number when data is available
                        byte[] buffer = new byte[bufferSize];
                        long bytesRead = source.read(buffer);

                        if (bytesRead == -1) {
                            // Stream ended (server sent endStream=true)
                            // This is the normal completion for HTTP/2 streaming
                            streamCallback.onComplete();
                            break;
                        } else if (bytesRead > 0) {
                            // Create exact-sized array if read less than buffer size
                            byte[] data = bytesRead < bufferSize
                                    ? Arrays.copyOf(buffer, (int) bytesRead)
                                    : buffer;

                            // Check if this might be the last chunk (non-blocking check)
                            // Note: For HTTP/2 streaming, isLast might be false even if
                            // this is the last chunk in current buffer, as more data may arrive later
                            boolean isLast = source.exhausted();

                            // Notify callback of data chunk
                            streamCallback.onData(data, isLast);
                        }
                        // If bytesRead == 0, continue loop to wait for more data
                    }

                } catch (IOException e) {
                    LOGGER.error("Error reading stream data: {}", e.getMessage(), e);
                    streamCallback.onError(e);
                } catch (Exception e) {
                    LOGGER.error("Unexpected error during stream processing: {}", e.getMessage(), e);
                    streamCallback.onError(e);
                } finally {
                    // Ensure response is closed to free resources
                    if (response != null) {
                        response.close();
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    streamCallback.onCancelled();
                } else {
                    LOGGER.error("Request failed: {}", e.getMessage(), e);
                    streamCallback.onError(e);
                }
            }
        });
    }
}
