/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.discovery.loadbalance;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alibaba.fescar.discovery.registry.RegistryFactory;
import com.alibaba.fescar.discovery.registry.RegistryService;

import org.junit.Assert;
import org.junit.Ignore;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The type Load balance factory test.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /02/12
 */
public class LoadBalanceFactoryTest {

    /**
     * Test get registry.
     *
     * @param loadBalance the load balance
     * @throws Exception the exception
     */
    @Test(dataProvider = "instanceProvider")
    @Ignore
    public void testGetRegistry(LoadBalance loadBalance) throws Exception {
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
     * @param loadBalance the load balance
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
     * @param loadBalance the load balance
     * @throws Exception the exception
     */
    @Test(dataProvider = "instanceProvider")
    @Ignore
    public void testSubscribe(LoadBalance loadBalance) throws Exception {
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
     * @param loadBalance the load balance
     * @throws Exception the exception
     */
    @Test(dataProvider = "instanceProvider")
    public void testGetAddress(LoadBalance loadBalance) throws Exception {
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
    @DataProvider
    public static Object[][] instanceProvider() {
        LoadBalance loadBalance = LoadBalanceFactory.getInstance();
        return new Object[][] {{loadBalance}};
    }

}