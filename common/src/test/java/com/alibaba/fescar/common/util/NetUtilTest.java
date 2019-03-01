/*
 *
 *  *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *
 *
 */

package com.alibaba.fescar.common.util;


import com.alibaba.fescar.common.util.NetUtil;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/2/26
 */
public class NetUtilTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void testToStringAddress() {
        String stringAddress = NetUtil.toStringAddress(InetSocketAddress.createUnresolved("127.0.0.1", 9828));
        Assert.assertEquals(stringAddress, "127.0.0.1:9828");
    }

    @Test(dataProvider = "inetAddress")
    public void testToStringAddress1(InetSocketAddress ipv4, InetSocketAddress ipv6) {
        Assert.assertEquals(NetUtil.toStringAddress(ipv4), ipv4.getAddress().getHostAddress() + ":" + ipv4.getPort());
        Assert.assertEquals(NetUtil.toStringAddress(ipv6), ipv6.getAddress().getHostAddress() + ":" + ipv6.getPort());
    }

    @Test(dataProvider = "inetAddress")
    public void testToStringAddress2(SocketAddress ipv4, SocketAddress ipv6) {
        Assert.assertEquals(NetUtil.toStringAddress(ipv4),
            ((InetSocketAddress) ipv4).getAddress().getHostAddress() + ":" + ((InetSocketAddress) ipv4).getPort());
        Assert.assertEquals(NetUtil.toStringAddress(ipv6),
            ((InetSocketAddress) ipv6).getAddress().getHostAddress() + ":" + ((InetSocketAddress) ipv6).getPort());
    }

    @Test(dataProvider = "inetAddress")
    public void testToIpAddress(InetSocketAddress ipv4, InetSocketAddress ipv6) {
        Assert.assertEquals(NetUtil.toIpAddress(ipv4), ipv4.getAddress().getHostAddress());
        Assert.assertEquals(NetUtil.toIpAddress(ipv6), ipv6.getAddress().getHostAddress());
    }


    @Test(expectedExceptions = NumberFormatException.class)
    public void testToInetSocketAddress() {
        Assert.assertNotNull(NetUtil.toInetSocketAddress("23939:ks"));
    }

    @Test
    public void testToInetSocketAddress1() {
        Assert.assertTrue(NetUtil.toInetSocketAddress("kadfskl").getHostName().equals("kadfskl"));
    }


    @Test(expectedExceptions = NullPointerException.class)
    public void testToLong() {
        NetUtil.toLong("kdskdsfk");
    }

    @Test
    public void testToLong1() {
        String[] split = "127.0.0.1".split("\\.");
        long r = 0;
        r = r | (Long.parseLong(split[0]) << 40);
        r = r | (Long.parseLong(split[1]) << 32);
        r = r | (Long.parseLong(split[2]) << 24);
        r = r | (Long.parseLong(split[3]) << 16);
        r = r | 0;
        Assert.assertEquals(NetUtil.toLong("127.0.0.1"), r);
    }


    @Test
    public void testGetLocalIp() {
        Assert.assertNotNull(NetUtil.getLocalIp());
    }

    @Test
    public void testGetLocalHost() {
        Assert.assertNotNull(NetUtil.getLocalHost());
    }

    @Test
    public void testGetLocalAddress() {
        Assert.assertNotNull(NetUtil.getLocalAddress());
    }

    @DataProvider
    public Object[][] inetAddress() throws UnknownHostException {
        return new Object[][]{{new InetSocketAddress(Inet4Address.getLocalHost().getHostName(), 3902),
            new InetSocketAddress(Inet6Address.getLocalHost().getHostName(), 3904)}};
    }
}