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
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.seata.common.executor.HttpCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Http5ClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http5ClientUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    public static final MediaType MEDIA_TYPE_FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded");

    public static void doPostHttp(
            String url, Map<String, String> params, Map<String, String> headers, HttpCallback<Response> callback) {
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

            executeAsync(HTTP_CLIENT, request, callback);

        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            callback.onFailure(e);
        }
    }

    public static void doPostHttp(
            String url, String body, Map<String, String> headers, HttpCallback<Response> callback) {
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

        executeAsync(HTTP_CLIENT, request, callback);
    }

    public static void doGetHttp(
            String url, Map<String, String> headers, final HttpCallback<Response> callback, int timeout) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();

        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers != null) {
            headers.forEach(headerBuilder::add);
        }

        Request request = new Request.Builder()
                .url(url)
                .headers(headerBuilder.build())
                .get()
                .build();

        executeAsync(client, request, callback);
    }

    private static RequestBody createRequestBody(Map<String, String> params, String contentType)
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
