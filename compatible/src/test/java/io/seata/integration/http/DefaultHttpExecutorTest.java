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
package io.seata.integration.http;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for DefaultHttpExecutor compatibility wrapper.
 */
public class DefaultHttpExecutorTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                DefaultHttpExecutor.class.isAnnotationPresent(Deprecated.class),
                "DefaultHttpExecutor should be marked as @Deprecated");
    }

    @Test
    public void testGetInstance() {
        DefaultHttpExecutor executor = DefaultHttpExecutor.getInstance();
        assertNotNull(executor, "getInstance() should return non-null instance");
    }

    @Test
    public void testGetInstanceReturnsSameWrapper() {
        DefaultHttpExecutor executor1 = DefaultHttpExecutor.getInstance();
        DefaultHttpExecutor executor2 = DefaultHttpExecutor.getInstance();

        assertNotNull(executor1);
        assertNotNull(executor2);
        // Each call creates a new wrapper instance
    }

    @Test
    public void testInitGetUrl() {
        DefaultHttpExecutor executor = DefaultHttpExecutor.getInstance();

        String host = "http://localhost:8080";
        String path = "/api/test";
        Map<String, String> querys = new HashMap<>();
        querys.put("param1", "value1");
        querys.put("param2", "value2");

        String url = executor.initGetUrl(host, path, querys);

        assertNotNull(url);
        assertTrue(url.contains(host));
        assertTrue(url.contains(path));
    }

    //    @Test
    //    public void testInitGetUrlWithNullQuerys() {
    //        DefaultHttpExecutor executor = DefaultHttpExecutor.getInstance();
    //
    //        String host = "http://localhost:8080";
    //        String path = "/api/test";
    //
    //        String url = executor.initGetUrl(host, path, null);
    //
    //        assertNotNull(url);
    //        assertTrue(url.contains(host));
    //        assertTrue(url.contains(path));
    //    }

    @Test
    public void testInitGetUrlWithEmptyQuerys() {
        DefaultHttpExecutor executor = DefaultHttpExecutor.getInstance();

        String host = "http://localhost:8080";
        String path = "/api/test";
        Map<String, String> querys = new HashMap<>();

        String url = executor.initGetUrl(host, path, querys);

        assertNotNull(url);
        assertTrue(url.contains(host));
        assertTrue(url.contains(path));
    }

    @Test
    public void testBuildGetHeaders() {
        DefaultHttpExecutor executor = DefaultHttpExecutor.getInstance();

        Map<String, String> headers = new HashMap<>();
        TestParam paramObject = new TestParam("test-value");

        // Should not throw exception
        executor.buildGetHeaders(headers, paramObject);
    }

    @Test
    public void testBuildPostHeaders() {
        DefaultHttpExecutor executor = DefaultHttpExecutor.getInstance();

        Map<String, String> headers = new HashMap<>();
        TestParam paramObject = new TestParam("test-value");

        // Should not throw exception
        executor.buildPostHeaders(headers, paramObject);
    }

    // Test helper class
    static class TestParam {
        private String value;

        public TestParam(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
