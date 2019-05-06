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
package io.seata.discovery.loadbalance;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.seata.discovery.registry.RegistryFactory;
import io.seata.discovery.registry.RegistryService;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * The type Load balance factory test.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /02/12
 */
@RunWith(Parameterized.class)
public class LoadBalanceFactoryTest {

    private LoadBalance loadBalance;

    public LoadBalanceFactoryTest(LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }

    /**
     * Test get registry.
     *
     * @throws Exception the exception
     */
    @Test
    @Ignore
    public void testGetRegistry() throws Exception {
        Assert.assertNotNull(loadBalance);
        RegistryService registryService = RegistryFactory.getInstance();
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8091);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 8092);
        registryService.register(address1);
        registryService.register(address2);
        List<InetSocketAddress> addressList = registryService.lookup("my_test_tx_group");
        InetSocketAddress balanceAddress = loadBalance.select(addressList);
        Assert.assertNotNull(balanceAddress);
    }

    /**
     * Test get address.
     *
     * @throws Exception the exception
     */
    @Test
    @Ignore
    public void testUnRegistry() throws Exception {
        RegistryService registryService = RegistryFactory.getInstance();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        registryService.unregister(address);
    }

    /**
     * Test subscribe.
     *
     * @throws Exception the exception
     */
    @Test
    @Ignore
    public void testSubscribe() throws Exception {
        Assert.assertNotNull(loadBalance);
        RegistryService registryService = RegistryFactory.getInstance();
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8091);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 8092);
        registryService.register(address1);
        registryService.register(address2);
        List<InetSocketAddress> addressList = registryService.lookup("my_test_tx_group");
        InetSocketAddress balanceAddress = loadBalance.select(addressList);
        Assert.assertNotNull(balanceAddress);
        TimeUnit.SECONDS.sleep(30);//等待testUnRegistry事件触发
        List<InetSocketAddress> addressList1 = registryService.lookup("my_test_tx_group");
        Assert.assertEquals(1, addressList1.size());
    }

    /**
     * Test get address.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetAddress() throws Exception {
        Assert.assertNotNull(loadBalance);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        List<InetSocketAddress> addressList = new ArrayList<>();
        addressList.add(address);
        InetSocketAddress balanceAddress = loadBalance.select(addressList);
        Assert.assertEquals(address, balanceAddress);
    }

    /**
     * Instance provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @Parameterized.Parameters
    public static Collection instanceProvider() {
        LoadBalance loadBalance = LoadBalanceFactory.getInstance();
        return Arrays.asList(loadBalance);
    }

}