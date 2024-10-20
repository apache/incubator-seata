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
package org.apache.seata.discovery.registry.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.QueryStringEncoder;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class OkHttpClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(OkHttpClientUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<Integer/*timeout*/, OkHttpClient> HTTP_CLIENT_MAP_V1 = new ConcurrentHashMap<>();
    private static final Map<Integer/*timeout*/, OkHttpClient> HTTP_CLIENT_MAP_V2 = new ConcurrentHashMap<>();

    public static Response doPostV1(String url, Map<String, String> params, Map<String, String> header,
                                    int timeout) throws IOException {
        OkHttpClient okHttpClient = HTTP_CLIENT_MAP_V1.computeIfAbsent(timeout, t -> new OkHttpClient()
                .newBuilder()
                .readTimeout(t, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build());

        return doPost(okHttpClient, url, params, header);
    }

    public static Response doPostV1(String url, String body, Map<String, String> header,
                                    int timeout) throws IOException {
        OkHttpClient okHttpClient = HTTP_CLIENT_MAP_V1.computeIfAbsent(timeout, t -> new OkHttpClient()
                .newBuilder()
                .readTimeout(t, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build());

        return doPost(okHttpClient, url, body, header);
    }

    public static Response doPostV2(String url, Map<String, String> params, Map<String, String> header,
                                    int timeout) throws IOException {
        OkHttpClient okHttpClient = HTTP_CLIENT_MAP_V2.computeIfAbsent(timeout, t -> new OkHttpClient()
                .newBuilder()
                .readTimeout(t, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE))
                .build());

        return doPost(okHttpClient, url, params, header);
    }

    private static Response doPost(OkHttpClient okHttpClient, String url, Map<String, String> params, Map<String, String> header) throws IOException {
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(url);
        params.forEach(queryStringEncoder::addParam);
        Request.Builder builder = new Request.Builder();
        String contentType = "";
        if (header != null) {
            header.forEach(builder::addHeader);
            contentType = header.get("Content-Type");
        }

        RequestBody requestBody = null;
        if (contentType != null && !contentType.isEmpty()) {
            if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                FormBody.Builder formBuilder = new FormBody.Builder();
                params.forEach(formBuilder::add);
                requestBody = formBuilder.build();
            } else if ("application/json".equalsIgnoreCase(contentType)) {
                String json = OBJECT_MAPPER.writeValueAsString(params);
                requestBody = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
            }
        }
        if (requestBody != null) {
            return okHttpClient.newCall(builder.url(queryStringEncoder.toString()).post(requestBody).build()).execute();
        }

        return okHttpClient.newCall(builder.url(queryStringEncoder.toString()).build()).execute();
    }

    private static Response doPost(OkHttpClient okHttpClient, String url, String body, Map<String, String> header) throws IOException {
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(url);
        Request.Builder builder = new Request.Builder();
        String contentType = "";
        if (header != null) {
            header.forEach(builder::addHeader);
            contentType = header.get("Content-Type");
        }

        RequestBody requestBody = null;
        if (contentType != null && !contentType.isEmpty()) {
            if ("application/json".equalsIgnoreCase(contentType)) {
                requestBody = RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"));
            }
        }
        if (requestBody != null) {
            return okHttpClient.newCall(builder.url(queryStringEncoder.toString()).post(requestBody).build()).execute();
        }

        return okHttpClient.newCall(builder.url(queryStringEncoder.toString()).build()).execute();
    }


    // get request
    public static Response doGet(String url, Map<String, String> param, Map<String, String> header,
                                 int timeout) throws IOException {
        OkHttpClient okHttpClient = HTTP_CLIENT_MAP_V1.computeIfAbsent(timeout, t -> new OkHttpClient()
                .newBuilder()
                .readTimeout(t, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build());

        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(url);
        if (param != null) {
            param.forEach(queryStringEncoder::addParam);
        }
        Request.Builder builder = new Request.Builder();
        header.forEach(builder::addHeader);
        Request request = builder.url(queryStringEncoder.toString()).build();
        return okHttpClient.newCall(request).execute();
    }

}
