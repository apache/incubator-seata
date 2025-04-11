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
package org.apache.seata.discovery.registry;

import static org.apache.seata.discovery.registry.RegistryService.CONFIG_SPLIT_CHAR;
import static org.apache.seata.discovery.registry.RegistryService.PREFIX_SERVICE_ROOT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockedStatic;
import java.net.InetSocketAddress;
import java.util.List;

public class FileRegistryServiceImplTest {

    @Test
    public void testLookup() throws Exception {
        String testCluster = "TEST-CLUSTER";
        String endpointDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + testCluster + ".grouplist";
        Configuration mockConfig = mock(Configuration.class);
        // mock normal case
        when(mockConfig.getConfig("service.vgroupMapping.test-group")).thenReturn(testCluster);
        when(mockConfig.getConfig(endpointDataId)).thenReturn("127.0.0.1:8080");
        try (MockedStatic<ConfigurationFactory> mockedFactory = mockStatic(ConfigurationFactory.class)) {
            mockedFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            FileRegistryServiceImpl localFileRegistryService = FileRegistryServiceImpl.getInstance();
            List<InetSocketAddress> inetSocketAddresses = localFileRegistryService.lookup("test-group");
            Assertions.assertEquals(inetSocketAddresses.size(),1);
            InetSocketAddress inetSocketAddress = inetSocketAddresses.get(0);
            Assertions.assertEquals(inetSocketAddress.getAddress().getHostAddress(),"127.0.0.1");
            Assertions.assertEquals(inetSocketAddress.getPort(),8080);
        }
        // mock exception case 1 : clusterName is null
        when(mockConfig.getConfig("service.vgroupMapping.test-group")).thenReturn(null);
        when(mockConfig.getConfig(endpointDataId)).thenReturn("127.0.0.1:8080");
        try (MockedStatic<ConfigurationFactory> mockedFactory = mockStatic(ConfigurationFactory.class)) {
            assertThrows(ConfigNotFoundException.class, ()->{
                mockedFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
                FileRegistryServiceImpl localFileRegistryService = FileRegistryServiceImpl.getInstance();
                localFileRegistryService.lookup("test-group");
            });

        }
        // mock exception case 2 : endpoint is null
        when(mockConfig.getConfig("service.vgroupMapping.test-group")).thenReturn(testCluster);
        when(mockConfig.getConfig(endpointDataId)).thenReturn(null);
        try (MockedStatic<ConfigurationFactory> mockedFactory = mockStatic(ConfigurationFactory.class)) {
            assertThrows(ConfigNotFoundException.class, ()->{
                mockedFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
                FileRegistryServiceImpl localFileRegistryService = FileRegistryServiceImpl.getInstance();
                localFileRegistryService.lookup("test-group");
            });
        }
    }

}
