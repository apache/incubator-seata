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

import io.netty.handler.codec.http.QueryStringEncoder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Http2ClientUtil {


    public static Response doPost(String url, Map<String, String> params, Map<String, String> header,
                                                        int timeout) throws IOException {
        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .readTimeout(timeout, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE))
                .build();
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(url);
        params.forEach(queryStringEncoder::addParam);
        Request.Builder builder = new Request.Builder();
        header.forEach(builder::addHeader);
        Request request = builder.url(queryStringEncoder.toString()).build();
        return client.newCall(request).execute();
    }
}
