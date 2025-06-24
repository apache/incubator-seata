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
package org.apache.seata.server.controller;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.server.DynamicPortTestConfig;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.common.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(DynamicPortTestConfig.class)
class ClusterControllerTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) {}

    @Test
    @Order(1)
    void watchTimeoutTest() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        header.put(HTTP.CONN_KEEP_ALIVE, "close");
        Map<String, String> param = new HashMap<>();
        param.put("default-test", "1");
        int port = Integer.parseInt(System.getProperty(SERVER_SERVICE_PORT_CAMEL, "8091"));
        try (CloseableHttpResponse response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", param, header, 5000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                Assertions.assertEquals(HttpStatus.SC_NOT_MODIFIED, statusLine.getStatusCode());
                return;
            }
        }
        Assertions.fail();
    }

    @Test
    @Order(2)
    void watch() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test", "1");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test", 2, true));
            }
        });
        thread.start();
        int port = Integer.parseInt(System.getProperty(SERVER_SERVICE_PORT_CAMEL, "8091"));
        try (CloseableHttpResponse response =
                HttpClientUtil.doPost("http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, 30000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                Assertions.assertEquals(HttpStatus.SC_OK, statusLine.getStatusCode());
                return;
            }
        }
        Assertions.fail();
    }
}
