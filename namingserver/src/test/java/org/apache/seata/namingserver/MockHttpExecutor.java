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
package org.apache.seata.namingserver;

import org.apache.seata.common.http.HttpExecutor;
import org.apache.seata.common.http.HttpResult;
import org.apache.seata.common.loader.LoadLevel;

import java.io.IOException;
import java.util.Map;

@LoadLevel(name = "Http2", order = 2)
public class MockHttpExecutor implements HttpExecutor {
    @Override
    public HttpResult doPost(String url, Map<String, String> params, Map<String, String> header, int timeout)
            throws IOException {
        HttpResult result = new HttpResult();
        result.setStatusCode(200);
        return result;
    }

    @Override
    public HttpResult doPost(String url, String body, Map<String, String> header, int timeout) throws IOException {
        HttpResult result = new HttpResult();
        result.setStatusCode(200);
        return result;
    }

    @Override
    public HttpResult doGet(String url, Map<String, String> param, Map<String, String> header, int timeout)
            throws IOException {
        HttpResult result = new HttpResult();
        result.setStatusCode(200);
        return result;
    }
}
