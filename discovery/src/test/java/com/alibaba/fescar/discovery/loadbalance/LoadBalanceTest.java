package com.alibaba.fescar.discovery.loadbalance;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by guoyao on 2019/2/14.
 */
public class LoadBalanceTest {


    @Test(dataProvider = "addressProvider")
    public void testRandomLoadBalance_select(List<InetSocketAddress> addresses) {
        int runs=10000;
        Map<InetSocketAddress, AtomicLong> counter=getSelectedCounter(runs, addresses, new RandomLoadBalance());
        for (InetSocketAddress address : counter.keySet()) {
            Long count=counter.get(address).get();
            Assert.assertTrue(count > 0, "selecte one time at last");
        }
    }

    @Test(dataProvider = "addressProvider")
    public void testRoundRobinLoadBalance_select(List<InetSocketAddress> addresses) {
        int runs=10000;
        Map<InetSocketAddress, AtomicLong> counter=getSelectedCounter(runs,addresses, new RoundRobinLoadBalance());
        for (InetSocketAddress address : counter.keySet()) {
            Long count=counter.get(address).get();
            Assert.assertTrue(Math.abs(count - runs / (0f + addresses.size())) < 1f,"abs diff shoud < 1");
        }
    }

    public Map<InetSocketAddress, AtomicLong> getSelectedCounter(int runs, List<InetSocketAddress> addresses, LoadBalance loadBalance) {
        Assert.assertNotNull(loadBalance);
        Map<InetSocketAddress, AtomicLong> counter=new ConcurrentHashMap<>();
        for (InetSocketAddress address : addresses) {
            counter.put(address, new AtomicLong(0));
        }
        try {
            for (int i=0; i < runs; i++) {
                InetSocketAddress selectAddress=loadBalance.select(addresses);
                counter.get(selectAddress).incrementAndGet();
            }
        } catch (Exception e) {
            //do nothing
        }
        return counter;
    }

    @DataProvider
    public static Object[][] addressProvider() {
        List<InetSocketAddress> addresses=Lists.newArrayList();
        addresses.add(new InetSocketAddress("127.0.0.1", 8091));
        addresses.add(new InetSocketAddress("127.0.0.1", 8092));
        addresses.add(new InetSocketAddress("127.0.0.1", 8093));
        addresses.add(new InetSocketAddress("127.0.0.1", 8094));
        addresses.add(new InetSocketAddress("127.0.0.1", 8095));
        return new Object[][]{{addresses}};
    }




}
