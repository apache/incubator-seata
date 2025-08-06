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
package org.apache.seata.core.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RateLimitEventTest
 */
public class RateLimitEventTest {
    @Test
    public void test() {
        String traceId = "trace123";
        String limitType = "GlobalBeginFailed";
        String applicationId = "app123";
        String serverIpAddressAndPort = "127.0.0.1:8080";

        RateLimitEvent event = new RateLimitEvent(traceId, limitType, applicationId, serverIpAddressAndPort);

        assertEquals(traceId, event.getTraceId());
        assertEquals(limitType, event.getLimitType());
        assertEquals(applicationId, event.getApplicationId());
        assertEquals(serverIpAddressAndPort, event.getServerIpAddressAndPort());

        event.setTraceId("newTraceId");
        event.setLimitType("NewLimitType");
        event.setApplicationId("newAppId");
        event.setServerIpAddressAndPort("192.168.1.1:9090");

        assertEquals("newTraceId", event.getTraceId());
        assertEquals("NewLimitType", event.getLimitType());
        assertEquals("newAppId", event.getApplicationId());
        assertEquals("192.168.1.1:9090", event.getServerIpAddressAndPort());
    }
}
