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
package org.apache.seata.discovery.registry.eureka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomEurekaInstanceConfigTest {
    private CustomEurekaInstanceConfig config;

    @BeforeEach
    void setUp() {
        config = new CustomEurekaInstanceConfig();
    }

    // test getInstanceId()
    @Test
    void testGetInstanceIdWhenSet() {
        config.setInstanceId("custom-instance-id");
        assertEquals("custom-instance-id", config.getInstanceId());
    }

    @Test
    void testGetInstanceIdWhenNotSet() throws Exception {
        String instanceId = (String) getConfigString("getInstanceId");
        assertEquals(instanceId, config.getInstanceId());
    }
    // test getIpAddress()
    @Test
    void testGetIpAddressWhenSet() {
        config.setIpAddress("192.168.1.1");
        assertEquals("192.168.1.1", config.getIpAddress());
    }

    @Test
    void testGetIpAddressWhenNotSet() throws Exception {
        String ipAddress = (String) getConfigString("getIpAddress");
        assertEquals(ipAddress, config.getIpAddress());
    }

    // test getNonSecurePort()
    @Test
    void testGetNonSecurePortWhenSet() {
        config.setPort(9090);
        assertEquals(9090, config.getNonSecurePort());
    }

    @Test
    void testGetNonSecurePortWhenNotSet() throws Exception {
        int  nonSecurePort = (int) getConfigString("getNonSecurePort");
        assertEquals(nonSecurePort, config.getNonSecurePort());
    }

    // test getAppname()
    @Test
    void testGetAppnameWhenSet() {
        config.setApplicationName("my-app");
        assertEquals("my-app", config.getAppname());
    }

    @Test
    void testGetAppnameWhenNotSet() throws Exception {
        String appName = (String) getConfigString("getAppname");
        assertEquals(appName, config.getAppname());
    }

    // test getHostName()
    @Test
    void testGetHostName() {
        config.setIpAddress("192.168.1.100");
        assertEquals("192.168.1.100", config.getHostName(true));
        assertEquals("192.168.1.100", config.getHostName(false));
    }

    @Test
    void testGetHostNameWhenIpAddressNotSet() throws Exception {
        String ipAddress = (String) getConfigString("getIpAddress");
        assertEquals(ipAddress, config.getHostName(false));
    }


    private Object getConfigString(String method) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?> grandparentClass = config.getClass().getSuperclass().getSuperclass();
        Method grandparentMethod = grandparentClass.getDeclaredMethod(method);
        return grandparentMethod.invoke(config);
    }

}

