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
package org.apache.seata.discovery.loadbalance;

import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The type Load balance factory test.
 */
public class LoadBalanceFactoryTest {

    private MockedStatic<ConfigurationFactory> mockedConfigurationFactory;

    @BeforeEach
    public void setUp() {
        mockedConfigurationFactory = mockStatic(ConfigurationFactory.class);
    }

    @AfterEach
    public void tearDown() {
        if (mockedConfigurationFactory != null) {
            mockedConfigurationFactory.close();
        }
    }

    @Test
    public void testGetInstance() {
        Configuration mockConfig = mock(Configuration.class);
        mockedConfigurationFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);

        when(mockConfig.getConfig(eq("client.loadBalance.type"), anyString())).thenReturn("XID");

        LoadBalance loadBalance = LoadBalanceFactory.getInstance();
        assertInstanceOf(XIDLoadBalance.class, loadBalance);
    }
}
