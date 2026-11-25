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
package org.apache.seata.core.rpc;

import org.apache.seata.common.XID;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.apache.seata.common.ConfigurationKeys.SERVER_REGISTRY_PORT_CAMEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdvertisedEndpointResolverTest {

    private static final int LISTEN_PORT = 18091;
    private static final int REGISTRY_PORT = 21908;

    private Configuration configuration;

    @BeforeEach
    public void setUp() {
        configuration = mock(Configuration.class);
        XID.setIpAddress("127.0.0.1");
        XID.setPort(LISTEN_PORT);
    }

    @Test
    public void testResolveTransactionPortFallsBackToListenPort() {
        when(configuration.getConfig(SERVER_REGISTRY_PORT_CAMEL)).thenReturn(null);

        assertEquals(LISTEN_PORT, resolveTransactionPort());
    }

    @Test
    public void testResolveTransactionPortUsesRegistryPortWhenItDiffersFromListenPort() {
        when(configuration.getConfig(SERVER_REGISTRY_PORT_CAMEL)).thenReturn(String.valueOf(REGISTRY_PORT));

        int port = resolveTransactionPort();

        assertEquals(REGISTRY_PORT, port);
        assertNotEquals(LISTEN_PORT, port);
    }

    @Test
    public void testResolveTransactionPortIgnoresInvalidRegistryPort() {
        when(configuration.getConfig(SERVER_REGISTRY_PORT_CAMEL)).thenReturn("not-a-port");

        assertEquals(LISTEN_PORT, resolveTransactionPort());
    }

    private int resolveTransactionPort() {
        try (MockedStatic<ConfigurationFactory> configurationFactory = Mockito.mockStatic(ConfigurationFactory.class)) {
            configurationFactory.when(ConfigurationFactory::getInstance).thenReturn(configuration);
            return AdvertisedEndpointResolver.resolveTransactionPort();
        }
    }
}
