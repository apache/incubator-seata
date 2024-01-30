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
    void testGet() throws NoSuchFieldException, IllegalAccessException, ExecutionException, InterruptedException, TimeoutException {
        // mainly test exception scene
        ConfigFuture configFuture = Mockito.spy(new ConfigFuture("file.conf", "defaultValue", ConfigFuture.ConfigOperation.GET));

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
}
