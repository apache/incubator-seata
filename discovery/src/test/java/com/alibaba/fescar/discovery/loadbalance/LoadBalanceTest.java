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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

/**
 * Created by guoyao on 2019/2/14.
 */
public class LoadBalanceTest {

    /**
     * Test random load balance select.
     *
     * @param addresses the addresses
     */
    @Test(dataProvider = "addressProvider")
    public void testRandomLoadBalance_select(List<InetSocketAddress> addresses) {
        int runs = 10000;
        Map<InetSocketAddress, AtomicLong> counter = getSelectedCounter(runs, addresses, new RandomLoadBalance());
        for (InetSocketAddress address : counter.keySet()) {
            Long count = counter.get(address).get();
            Assert.assertTrue(count > 0, "selecte one time at last");
        }
    }

    /**
     * Test round robin load balance select.
     *
     * @param addresses the addresses
     */
    @Test(dataProvider = "addressProvider")
    public void testRoundRobinLoadBalance_select(List<InetSocketAddress> addresses) {
        int runs = 10000;
        Map<InetSocketAddress, AtomicLong> counter = getSelectedCounter(runs, addresses, new RoundRobinLoadBalance());
        for (InetSocketAddress address : counter.keySet()) {
            Long count = counter.get(address).get();
            Assert.assertTrue(Math.abs(count - runs / (0f + addresses.size())) < 1f, "abs diff shoud < 1");
        }
    }

    /**
     * Gets selected counter.
     *
     * @param runs        the runs
     * @param addresses   the addresses
     * @param loadBalance the load balance
     * @return the selected counter
     */
    public Map<InetSocketAddress, AtomicLong> getSelectedCounter(int runs, List<InetSocketAddress> addresses,
                                                                 LoadBalance loadBalance) {
        Assert.assertNotNull(loadBalance);
        Map<InetSocketAddress, AtomicLong> counter = new ConcurrentHashMap<>();
        for (InetSocketAddress address : addresses) {
            counter.put(address, new AtomicLong(0));
        }
        try {
            for (int i = 0; i < runs; i++) {
                InetSocketAddress selectAddress = loadBalance.select(addresses);
                counter.get(selectAddress).incrementAndGet();
            }
        } catch (Exception e) {
            //do nothing
        }
        return counter;
    }

    /**
     * Address provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] addressProvider() {
        List<InetSocketAddress> addresses = Lists.newArrayList();
        addresses.add(new InetSocketAddress("127.0.0.1", 8091));
        addresses.add(new InetSocketAddress("127.0.0.1", 8092));
        addresses.add(new InetSocketAddress("127.0.0.1", 8093));
        addresses.add(new InetSocketAddress("127.0.0.1", 8094));
        addresses.add(new InetSocketAddress("127.0.0.1", 8095));
        return new Object[][] {{addresses}};
    }

}
