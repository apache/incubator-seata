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
package org.apache.seata.integration.http;

import org.apache.seata.core.context.RootContext;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.Args;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MockHttpExecuter extends AbstractHttpExecutor {

    DefaultHttpExecutor httpExecutor = DefaultHttpExecutor.getInstance();

    @Override
    public <K> K executeGet(String host, String path, Map<String, String> paramObject, Class<K> returnType) throws IOException {
        Args.notNull(host, "host");
        Args.notNull(path, "path");

        String getUrl = initGetUrl(host, path, paramObject);
        Map<String, String> headers = new HashMap<>();

        MockRequest mockRequest = new MockRequest(getUrl, headers, null, path, "get");
        MockResponse mockResponse = new MockResponse(null);
        String xid = RootContext.getXID();
        if (xid != null) {
            headers.put(RootContext.KEY_XID, xid);
        }
        MockWebServer webServer =  new MockWebServer();
        webServer.initServletMapping();
        return (K) webServer.dispatch(mockRequest, mockResponse);
    }

    @Override
    protected <T> void buildClientEntity(CloseableHttpClient httpClient, T paramObject) {

    }

    @Override
    protected <T> void buildGetHeaders(Map<String, String> headers, T paramObject) {

    }

    @Override
    protected String initGetUrl(String host, String path, Map<String, String> paramObject) {
        return httpExecutor.initGetUrl(host, path, paramObject);
    }

    @Override
    protected <T> void buildPostHeaders(Map<String, String> headers, T t) {

    }

    @Override
    protected <T> StringEntity buildEntity(StringEntity entity, T t) {
        return null;
    }

    @Override
    protected <K> K convertResult(HttpResponse response, Class<K> clazz) {
        return null;
    }


}
