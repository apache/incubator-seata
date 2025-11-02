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
package org.apache.seata.config;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.ReflectionUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class ConfigFutureTest {

    @Test
    void testGet()
            throws NoSuchFieldException, IllegalAccessException, ExecutionException, InterruptedException,
                    TimeoutException {
        // mainly test exception scene
        ConfigFuture configFuture =
                Mockito.spy(new ConfigFuture("file.conf", "defaultValue", ConfigFuture.ConfigOperation.GET));

        Field originField = ReflectionUtil.getField(ConfigFuture.class, "origin");
        CompletableFuture<Object> origin = (CompletableFuture<Object>) originField.get(configFuture);
        // mock field
        origin = Mockito.spy(origin);
        // set mocked field to object
        originField.setAccessible(true);
        originField.set(configFuture, origin);

        Mockito.doThrow(ExecutionException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertThrows(ShouldNeverHappenException.class, configFuture::get);

        Mockito.doThrow(TimeoutException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals("defaultValue", configFuture.get());

        Mockito.doThrow(InterruptedException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals("defaultValue", configFuture.get());

        Mockito.doReturn(null).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals("defaultValue", configFuture.get());

        // set another config operation
        configFuture.setOperation(ConfigFuture.ConfigOperation.PUT);

        Mockito.doThrow(ExecutionException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertThrows(ShouldNeverHappenException.class, configFuture::get);

        Mockito.doThrow(TimeoutException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals(Boolean.FALSE, configFuture.get());

        Mockito.doThrow(InterruptedException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals(Boolean.FALSE, configFuture.get());

        Mockito.doReturn(null).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals(Boolean.FALSE, configFuture.get());
    }

    @Test
    void setDataId() {
        ConfigFuture configFuture = new ConfigFuture("file.conf", "defaultValue", ConfigFuture.ConfigOperation.GET);
        Assertions.assertEquals("file.conf", configFuture.getDataId());
        configFuture.setDataId("file-test.conf");
        Assertions.assertEquals("file-test.conf", configFuture.getDataId());
    }

    @Test
    void setContent() {
        ConfigFuture configFuture = new ConfigFuture("file.conf", "defaultValue", ConfigFuture.ConfigOperation.GET);
        Assertions.assertEquals("defaultValue", configFuture.getContent());
        configFuture.setContent("testValue");
        Assertions.assertEquals("testValue", configFuture.getContent());
    }

    @Test
    void testConstructorWithDefaultTimeout() {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.GET);
        Assertions.assertNotNull(configFuture);
        Assertions.assertEquals("test.conf", configFuture.getDataId());
        Assertions.assertEquals("default", configFuture.getContent());
        Assertions.assertEquals(ConfigFuture.ConfigOperation.GET, configFuture.getOperation());
    }

    @Test
    void testConstructorWithCustomTimeout() {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.PUT, 10000);
        Assertions.assertNotNull(configFuture);
    }

    @Test
    void testSetOperation() {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.GET);
        Assertions.assertEquals(ConfigFuture.ConfigOperation.GET, configFuture.getOperation());

        configFuture.setOperation(ConfigFuture.ConfigOperation.PUT);
        Assertions.assertEquals(ConfigFuture.ConfigOperation.PUT, configFuture.getOperation());

        configFuture.setOperation(ConfigFuture.ConfigOperation.PUTIFABSENT);
        Assertions.assertEquals(ConfigFuture.ConfigOperation.PUTIFABSENT, configFuture.getOperation());

        configFuture.setOperation(ConfigFuture.ConfigOperation.REMOVE);
        Assertions.assertEquals(ConfigFuture.ConfigOperation.REMOVE, configFuture.getOperation());
    }

    @Test
    void testRemoveOperation() throws Exception {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.REMOVE);

        Field originField = ReflectionUtil.getField(ConfigFuture.class, "origin");
        CompletableFuture<Object> origin = (CompletableFuture<Object>) originField.get(configFuture);
        origin = Mockito.spy(origin);
        originField.setAccessible(true);
        originField.set(configFuture, origin);

        Mockito.doThrow(TimeoutException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals(Boolean.FALSE, configFuture.get());
    }

    @Test
    void testPutIfAbsentOperation() throws Exception {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.PUTIFABSENT);

        Field originField = ReflectionUtil.getField(ConfigFuture.class, "origin");
        CompletableFuture<Object> origin = (CompletableFuture<Object>) originField.get(configFuture);
        origin = Mockito.spy(origin);
        originField.setAccessible(true);
        originField.set(configFuture, origin);

        Mockito.doThrow(TimeoutException.class).when(origin).get(Mockito.anyLong(), Mockito.any());
        Assertions.assertEquals(Boolean.FALSE, configFuture.get());
    }

    @Test
    void testSetOrigin() throws Exception {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.GET);

        Field originField = ReflectionUtil.getField(ConfigFuture.class, "origin");
        CompletableFuture<Object> origin = (CompletableFuture<Object>) originField.get(configFuture);

        origin.complete("completed-value");
        Object result = configFuture.get();
        Assertions.assertEquals("completed-value", result);
    }

    @Test
    void testSetTimeoutMills() {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.GET, 20000);
        Assertions.assertNotNull(configFuture);
    }

    @Test
    void testGetWithSuccessfulCompletion() throws Exception {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.GET);

        Field originField = ReflectionUtil.getField(ConfigFuture.class, "origin");
        CompletableFuture<Object> origin = (CompletableFuture<Object>) originField.get(configFuture);

        origin.complete("success-value");
        Object result = configFuture.get();
        Assertions.assertEquals("success-value", result);
    }

    @Test
    void testGetWithBooleanResult() throws Exception {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.PUT);

        Field originField = ReflectionUtil.getField(ConfigFuture.class, "origin");
        CompletableFuture<Object> origin = (CompletableFuture<Object>) originField.get(configFuture);

        origin.complete(Boolean.TRUE);
        Object result = configFuture.get();
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testMultipleGetCalls() throws Exception {
        ConfigFuture configFuture = new ConfigFuture("test.conf", "default", ConfigFuture.ConfigOperation.GET);

        Field originField = ReflectionUtil.getField(ConfigFuture.class, "origin");
        CompletableFuture<Object> origin = (CompletableFuture<Object>) originField.get(configFuture);

        origin.complete("value");
        Object result1 = configFuture.get();
        Object result2 = configFuture.get();

        Assertions.assertEquals("value", result1);
        Assertions.assertEquals("value", result2);
    }

    @Test
    void testConfigOperationEnum() {
        Assertions.assertNotNull(ConfigFuture.ConfigOperation.GET);
        Assertions.assertNotNull(ConfigFuture.ConfigOperation.PUT);
        Assertions.assertNotNull(ConfigFuture.ConfigOperation.PUTIFABSENT);
        Assertions.assertNotNull(ConfigFuture.ConfigOperation.REMOVE);

        Assertions.assertEquals("GET", ConfigFuture.ConfigOperation.GET.name());
        Assertions.assertEquals("PUT", ConfigFuture.ConfigOperation.PUT.name());
        Assertions.assertEquals("PUTIFABSENT", ConfigFuture.ConfigOperation.PUTIFABSENT.name());
        Assertions.assertEquals("REMOVE", ConfigFuture.ConfigOperation.REMOVE.name());
    }
}
