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
package io.seata.integration.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.seata.common.util.CollectionUtils;
import io.seata.core.context.RootContext;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract http executor.
 *
 * @author wangxb
 */
public abstract class AbstractHttpExecutor implements HttpExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpExecutor.class);

    @Override
    public <T, K> K executePost(String host, String path, T paramObject, Class<K> returnType) throws IOException {
        Args.notNull(returnType, "returnType");
        HttpPost httpPost = new HttpPost(host + path);
        StringEntity entity = execute(host, path, paramObject);
        if (entity != null) {
            httpPost.setEntity(entity);
        }
        Map<String, String> headers = new HashMap<>();
        buildPostHeaders(headers, paramObject);
        CloseableHttpClient httpClient = initHttpClientInstance(paramObject);
        return wrapHttpExecute(returnType, httpClient, httpPost, headers);
    }

    @Override
    public <T, K> K executePut(String host, String path, T paramObject, Class<K> returnType) throws IOException {
        Args.notNull(returnType, "returnType");
        HttpPut httpPut = new HttpPut(host + path);
        StringEntity entity = execute(host, path, paramObject);
        if (entity != null) {
            httpPut.setEntity(entity);
        }
        Map<String, String> headers = new HashMap<>();
        buildPostHeaders(headers, paramObject);
        CloseableHttpClient httpClient = initHttpClientInstance(paramObject);
        return wrapHttpExecute(returnType, httpClient, httpPut, headers);
    }

    private <T> StringEntity execute(String host, String path, T paramObject) {
        Args.notNull(host, "host");
        Args.notNull(path, "path");
        StringEntity entity = null;
        if (paramObject != null) {
            String content;
            if (paramObject instanceof String) {
                String sParam = (String) paramObject;
                JSONObject jsonObject = null;
                try {
                    jsonObject = JSON.parseObject(sParam);
                    content = jsonObject.toJSONString();
                } catch (JSONException e) {
                    //Interface provider process parse exception
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(e.getMessage());
                    }
                    content = sParam;
                }

            } else {
                content = JSON.toJSONString(paramObject);
            }
            entity = new StringEntity(content, ContentType.APPLICATION_JSON);
        }

        return buildEntity(entity, paramObject);
    }

    @Override
    public <K> K executeGet(String host, String path, Map<String, String> paramObject, Class<K> returnType) throws IOException {

        Args.notNull(returnType, "returnType");
        Args.notNull(host, "host");
        Args.notNull(path, "path");

        CloseableHttpClient httpClient = initHttpClientInstance(paramObject);

        HttpGet httpGet = new HttpGet(initGetUrl(host, path, paramObject));
        Map<String, String> headers = new HashMap<>();

        buildGetHeaders(headers, paramObject);
        return wrapHttpExecute(returnType, httpClient, httpGet, headers);
    }

    private <T> CloseableHttpClient initHttpClientInstance(T paramObject) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        buildClientEntity(httpClient, paramObject);
        return httpClient;
    }

    protected abstract <T> void buildClientEntity(CloseableHttpClient httpClient, T paramObject);

    private <K> K wrapHttpExecute(Class<K> returnType, CloseableHttpClient httpClient, HttpUriRequest httpUriRequest,
                                  Map<String, String> headers) throws IOException {
        CloseableHttpResponse response;
        String xid = RootContext.getXID();
        if (xid != null) {
            headers.put(RootContext.KEY_XID, xid);
        }
        if (!headers.isEmpty()) {
            headers.forEach(httpUriRequest::addHeader);
        }
        response = httpClient.execute(httpUriRequest);
        int statusCode = response.getStatusLine().getStatusCode();
        /** 2xx is success. */
        if (statusCode < HttpStatus.SC_OK || statusCode > HttpStatus.SC_MULTI_STATUS) {
            throw new RuntimeException("Failed to invoke the http method "
                    + httpUriRequest.getURI() + " in the service "
                    + ". return status by: " + response.getStatusLine().getStatusCode());
        }

        return convertResult(response, returnType);
    }

    protected abstract <T> void buildGetHeaders(Map<String, String> headers, T paramObject);

    protected abstract String initGetUrl(String host, String path, Map<String, String> paramObject);


    protected abstract <T> void buildPostHeaders(Map<String, String> headers, T t);

    protected abstract <T> StringEntity buildEntity(StringEntity entity, T t);

    protected abstract <K> K convertResult(HttpResponse response, Class<K> clazz);


    public static Map<String, String> convertParamOfBean(Object sourceParam) {
        return CollectionUtils.toStringMap(JSON.parseObject(JSON.toJSONString(sourceParam, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteMapNullValue), Map.class));
    }

    @SuppressWarnings("lgtm[java/unsafe-deserialization]")
    public static <T> Map<String, String> convertParamOfJsonString(String jsonStr, Class<T> returnType) {
        return convertParamOfBean(JSON.parseObject(jsonStr, returnType));
    }
}
