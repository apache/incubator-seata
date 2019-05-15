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
package io.seata.integration.sofa.rpc;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import io.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Geng Zhang
 */
public class TransactionContextFilterTest {

    @Test
    public void testAll() {
        HelloServiceImpl helloServiceImpl;
        HelloService helloServiceRef;
        HelloServiceProxy helloServiceProxy;
        HelloService helloService;

        // mock A -> B -> C

        { // C
            ServerConfig serverConfig1 = new ServerConfig()
                    .setStopTimeout(0).setPort(22222)
                    .setQueues(5).setCoreThreads(1).setMaxThreads(1);
            helloServiceImpl = new HelloServiceImpl();
            ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
                    .setInterfaceId(HelloService.class.getName())
                    .setRef(helloServiceImpl)
                    .setServer(serverConfig1)
                    .setUniqueId("x1")
                    .setRegister(false);
            providerConfig.export();
        }
        { // B
            ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
                    .setInterfaceId(HelloService.class.getName())
                    .setTimeout(1000)
                    .setDirectUrl("bolt://127.0.0.1:22222")
                    .setUniqueId("x1")
                    .setRegister(false);
            helloServiceRef = consumerConfig.refer();

            ServerConfig serverConfig2 = new ServerConfig()
                    .setStopTimeout(0).setPort(22223)
                    .setQueues(5).setCoreThreads(1).setMaxThreads(1);
            helloServiceProxy = new HelloServiceProxy(helloServiceRef);
            ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
                    .setInterfaceId(HelloService.class.getName())
                    .setRef(helloServiceProxy)
                    .setServer(serverConfig2)
                    .setUniqueId("x2")
                    .setRegister(false);
            providerConfig.export();
        }
        { // A
            ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
                    .setInterfaceId(HelloService.class.getName())
                    .setTimeout(1000)
                    .setDirectUrl("bolt://127.0.0.1:22223")
                    .setUniqueId("x2")
                    .setRegister(false);
            helloService = consumerConfig.refer();
        }

        try {
            helloService.sayHello("xxx", 22);
            // check C
            Assertions.assertNull(helloServiceImpl.getXid());
            // check B
            Assertions.assertNull(helloServiceProxy.getXid());
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SofaRpcException);
        } finally {
            Assertions.assertNull(RootContext.unbind());
        }

        RootContext.bind("xidddd");
        try {
            helloService.sayHello("xxx", 22);
            // check C
            Assertions.assertEquals(helloServiceImpl.getXid(), "xidddd");
            // check B
            Assertions.assertEquals(helloServiceProxy.getXid(), "xidddd");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SofaRpcException);
        } finally {
            Assertions.assertEquals("xidddd", RootContext.unbind());
        }
    }

    @BeforeAll
    public static void adBeforeClass() {
        RpcRunningState.setUnitTestMode(true);
    }

    @AfterAll
    public static void adAfterClass() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }
}