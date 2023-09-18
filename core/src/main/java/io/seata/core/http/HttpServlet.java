/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServlet.class);

    // post request
    public static CloseableHttpResponse doPost(String url, String jsonBody) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);


            StringEntity entity = new StringEntity(jsonBody);


            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(entity);

            CloseableHttpResponse response = httpClient.execute(httpPost);


            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // get request
    public static CloseableHttpResponse doGet(String url, Map<String, String> queryParams) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();


            URIBuilder uriBuilder = new URIBuilder(url);
            if (queryParams != null) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }

            HttpGet httpGet = new HttpGet(uriBuilder.build());

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000)
                    .setSocketTimeout(5000)
                    .build();
            httpGet.setConfig(requestConfig);
            httpGet.setHeader("Content-Type", "application/json");

            CloseableHttpResponse response = httpClient.execute(httpGet);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}