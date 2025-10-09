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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.seata.common.executor.HttpCallback;
import org.apache.seata.common.loader.LoadLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@LoadLevel(name = "Http2", order = 2)
public class Http2HttpExecutor implements HttpExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2HttpExecutor.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    public static final MediaType MEDIA_TYPE_FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded");

    @Override
    public HttpResult<Response> doPost(String url, Map<String, String> params, Map<String, String> headers, int timeout)
            throws IOException {
        try {
            Headers.Builder headerBuilder = new Headers.Builder();
            if (headers != null) {
                headers.forEach(headerBuilder::add);
            }

            String contentType = headers != null ? headers.get("Content-Type") : "";
            RequestBody requestBody = createRequestBody(params, contentType);

            Request request = new Request.Builder()
                    .url(url)
                    .headers(headerBuilder.build())
                    .post(requestBody)
                    .build();

            CompletableFuture<HttpResult<Response>> future = new CompletableFuture<>();
            HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String responseBody =
                                response.body() != null ? response.body().string() : null;
                        future.complete(new HttpResult<>(response.code(), responseBody, response));
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    } finally {
                        response.close(); // 确保关闭
                    }
                }
            });

            // 阻塞等待回调完成，统一同步 API
            try {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new IOException("HTTP2 request failed", e);
            }

        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException("Failed to serialize request params to JSON", e);
        }
    }

    @Override
    public HttpResult<Response> doPost(String url, String body, Map<String, String> headers, int timeout)
            throws IOException {
        try {
            Headers.Builder headerBuilder = new Headers.Builder();
            if (headers != null) {
                headers.forEach(headerBuilder::add);
            }

            RequestBody requestBody = RequestBody.create(body, MEDIA_TYPE_JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .headers(headerBuilder.build())
                    .post(requestBody)
                    .build();

            CompletableFuture<HttpResult<Response>> future = new CompletableFuture<>();
            HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String responseBody =
                                response.body() != null ? response.body().string() : null;
                        future.complete(new HttpResult<>(response.code(), responseBody, response));
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    } finally {
                        response.close(); // 确保关闭
                    }
                }
            });

            // 阻塞等待结果
            try {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new IOException("HTTP2 POST request failed or timed out", e);
            }
        } catch (Exception e) {
            throw new IOException("Failed to execute HTTP2 POST request", e);
        }
    }

    @Override
    public HttpResult doGet(String url, Map<String, String> param, Map<String, String> header, int timeout)
            throws IOException {
        // todo
        return null;
    }

    @Override
    public HttpResult<Response> doGet(String url, Map<String, String> headers, int timeout) throws IOException {
        try {
            Headers.Builder headerBuilder = new Headers.Builder();
            if (headers != null) {
                headers.forEach(headerBuilder::add);
            }

            Request request = new Request.Builder()
                    .url(url)
                    .headers(headerBuilder.build())
                    .get()
                    .build();

            CompletableFuture<HttpResult<Response>> future = new CompletableFuture<>();
            HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String responseBody =
                                response.body() != null ? response.body().string() : null;
                        future.complete(new HttpResult<>(response.code(), responseBody, response));
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    } finally {
                        response.close(); // 确保关闭
                    }
                }
            });

            // 阻塞等待结果
            try {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new IOException("HTTP2 GET request failed or timed out", e);
            }
        } catch (Exception e) {
            throw new IOException("Failed to execute HTTP2 GET request", e);
        }
    }

    @Override
    public HttpResult doPostJson(String url, String jsonBody, Map<String, String> headers, int timeout)
            throws IOException {
        return null;
    }

    private RequestBody createRequestBody(Map<String, String> params, String contentType)
            throws JsonProcessingException {
        if (params == null || params.isEmpty()) {
            return RequestBody.create(new byte[0]);
        }

        if (MEDIA_TYPE_FORM_URLENCODED.toString().equals(contentType)) {
            FormBody.Builder formBuilder = new FormBody.Builder();
            params.forEach(formBuilder::add);
            return formBuilder.build();
        } else {
            String json = OBJECT_MAPPER.writeValueAsString(params);
            return RequestBody.create(json, MEDIA_TYPE_JSON);
        }
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
}
