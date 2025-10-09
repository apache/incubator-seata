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

import org.apache.seata.common.http.Http1HttpExecutor;
import org.apache.seata.common.http.HttpResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Http1HttpExecutorTest {

    @Test
    public void testDoPost() throws IOException {
        Assertions.assertNull(Http1HttpExecutor.getInstance().doPost("test", new HashMap<>(), new HashMap<>(), 0));
        Assertions.assertNull(Http1HttpExecutor.getInstance().doGet("test", new HashMap<>(), new HashMap<>(), 0));
    }

    @Test
    void testDoGetBaidu() throws Exception {
        HttpResult<Void> httpResult = Http1HttpExecutor.getInstance().doGet("https://www.baidu.com", null, null, 5000);

        assertNotNull(httpResult);
        assertEquals(200, httpResult.getStatusCode());
    }

    @Test
    void testDoPostNormal() throws Exception {
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        HttpResult<Void> httpResult = Http1HttpExecutor.getInstance()
                .doPost("https://postman-echo.com/post", "{\"name\":\"seata\"}", header, 5000);

        assertNotNull(httpResult);
        assertEquals(200, httpResult.getStatusCode());
        assertTrue(httpResult.getResponseBody().contains("seata"));
    }
}
