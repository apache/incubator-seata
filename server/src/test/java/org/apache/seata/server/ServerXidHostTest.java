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
package org.apache.seata.server;

import org.apache.seata.common.util.NetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Test the host selection that feeds {@link org.apache.seata.common.XID}.
 */
public class ServerXidHostTest {

    /**
     * An ip literal is stored as is, no resolution happens.
     */
    @Test
    public void testResolveXidHostKeepsIpLiteral() {
        Assertions.assertEquals("8.210.212.91", Server.resolveXidHost("8.210.212.91"));
    }

    /**
     * Guards the regression reported in #8158: a configured host name must be resolved to its ip literal
     * before it is stored, otherwise the xid carries the host name and XIDLoadBalance cannot match the tc.
     */
    @Test
    public void testResolveXidHostConvertsHostNameToIpLiteral() throws UnknownHostException {
        // the local host name is the only name guaranteed to resolve without external dns
        String hostName = InetAddress.getLocalHost().getHostName();
        // skip only when the host name is literally an ip, then there is nothing to convert. note that
        // NetUtil.isValidIp resolves the name first, so it cannot be used to detect a literal here.
        Assumptions.assumeFalse(
                NetUtil.isValidIPv4(hostName) || NetUtil.isValidIPv6(hostName), "host name is already an ip literal");

        String expected = InetAddress.getByName(hostName).getHostAddress();
        // resolveXidHost keeps the resolved literal only when it is a routable (non-forbidden) address;
        // a loopback resolution would take the local-ip fallback path instead
        Assumptions.assumeTrue(NetUtil.isValidIp(expected, false), "host name resolves to a forbidden address");

        // the regression: the value stored in the xid is the resolved ip literal, not the host name
        Assertions.assertEquals(expected, Server.resolveXidHost(hostName));
    }
}
