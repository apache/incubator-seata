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
package org.apache.seata.config.etcd3;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EtcdConfigurationTest {

    private Configuration mockFileConfig;

    private Client mockClient;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        System.setProperty("seataEnv", "test");
        mockClient = mock(Client.class);
        mockFileConfig = mock(Configuration.class);
        when(mockFileConfig.getConfig(anyString(), anyString())).thenReturn("seata.properties");
        Field clientField = EtcdConfiguration.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(null, mockClient);
    }

    @Test
    void testOnNext_skipWhenValueIsBlank() {

        KeyValue keyValue = mock(KeyValue.class);
        when(keyValue.getValue()).thenReturn(ByteSequence.from(new byte[0]));

        WatchEvent event = mock(WatchEvent.class);
        when(event.getKeyValue()).thenReturn(keyValue);

        WatchResponse watchResponse = mock(WatchResponse.class);
        when(watchResponse.getEvents()).thenReturn(Collections.singletonList(event));

        ConfigurationChangeListener mockListener = mock(ConfigurationChangeListener.class);
        String dataId = "seata.properties";
        EtcdConfiguration.EtcdListener etcdListener = new EtcdConfiguration.EtcdListener(dataId, mockListener);

        Watch.Listener listener = getEtcdListenerInnerListener(etcdListener);
        listener.onNext(watchResponse);

        verify(mockListener, never()).onProcessEvent(any());
    }

    /**
     * The original logic is defined in an anonymous inner class. It is impossible to directly obtain the listener and call it, so it can only be mocked here
     */
    private Watch.Listener getEtcdListenerInnerListener(EtcdConfiguration.EtcdListener etcdListener) {

        return new Watch.Listener() {
            @Override
            public void onNext(WatchResponse watchResponse) {
                byte[] bytes = watchResponse
                        .getEvents()
                        .get(0)
                        .getKeyValue()
                        .getValue()
                        .getBytes();
                if (bytes == null || bytes.length == 0) {
                    return;
                }
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onCompleted() {}
        };
    }
}
