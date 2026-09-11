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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

/**
 * Default http executor.
 *
 * @author wangxb
 */
public class DefaultHttpExecutor extends AbstractHttpExecutor {

    private static DefaultHttpExecutor instance = new DefaultHttpExecutor();

    private DefaultHttpExecutor() {
    }

    public static DefaultHttpExecutor getInstance() {
        return instance;
    }

    @Override
    public <T> void buildClientEntity(CloseableHttpClient httpClient, T paramObject) {

    }

    @Override
    public <T> void buildGetHeaders(Map<String, String> headers, T paramObject) {

    }


    @Override
    public String initGetUrl(String host, String path, Map<String, String> querys) {

        if (querys.isEmpty()) {
            return host + path;
        }
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(host);
        if (!StringUtils.isBlank(path)) {
            sbUrl.append(path);
        }

        StringBuilder sbQuery = new StringBuilder();
        Iterator queryKeys = querys.entrySet().iterator();

        while (queryKeys.hasNext()) {
            Map.Entry<String, String> query = (Map.Entry) queryKeys.next();
            if (0 < sbQuery.length()) {
                sbQuery.append("&");
            }

            if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
                sbQuery.append(query.getValue());
            }

            if (!StringUtils.isBlank(query.getKey())) {
                sbQuery.append(query.getKey());
                if (!StringUtils.isBlank(query.getValue())) {
                    sbQuery.append("=");
                    try {
                        sbQuery.append(URLEncoder.encode(query.getValue(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        }

        if (sbQuery.length() > 0) {
            sbUrl.append("?").append(sbQuery);
        }

        return sbUrl.toString();

    }

    @Override
    public <T> void buildPostHeaders(Map<String, String> headers, T t) {

    }

    @Override
    public <T> StringEntity buildEntity(StringEntity entity, T t) {
        return entity;
    }

    @Override
    public <K> K convertResult(HttpResponse response, Class<K> clazz) {


        if (clazz == HttpResponse.class) {
            return (K) response;
        }
        return null;
    }

}
