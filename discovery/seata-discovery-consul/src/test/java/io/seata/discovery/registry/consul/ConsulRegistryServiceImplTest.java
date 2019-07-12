/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.discovery.registry.consul;

import io.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/**
 * @author xingfudeshi@gmail.com
 * @date 2019/04/04
 */
public class ConsulRegistryServiceImplTest {


    @Test
    public void testRegister() throws Exception {
        RegistryService registryService = mock(ConsulRegistryServiceImpl.class);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8091);
        registryService.register(inetSocketAddress);
        verify(registryService).register(inetSocketAddress);
    }

    @Test
    public void testUnregister() throws Exception {
        RegistryService registryService = mock(ConsulRegistryServiceImpl.class);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8091);
        registryService.unregister(inetSocketAddress);
        verify(registryService).unregister(inetSocketAddress);
    }

    @Test
    public void testSubscribe() throws Exception {
        RegistryService registryService = mock(ConsulRegistryServiceImpl.class);
        ConsulListener consulListener = mock(ConsulListener.class);
        registryService.subscribe("test", consulListener);
        verify(registryService).subscribe("test", consulListener);
    }

    @Test
    public void testLookup() throws Exception {
        RegistryService registryService = mock(ConsulRegistryServiceImpl.class);
        registryService.lookup("test-key");
        verify(registryService).lookup("test-key");
    }
}
