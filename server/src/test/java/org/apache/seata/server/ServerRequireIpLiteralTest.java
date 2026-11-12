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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Server#requireIpLiteral(String)}.
 */
public class ServerRequireIpLiteralTest {

    @Test
    public void testAcceptsIpv4Literal() {
        Assertions.assertDoesNotThrow(() -> Server.requireIpLiteral("192.168.1.10"));
        Assertions.assertDoesNotThrow(() -> Server.requireIpLiteral("127.0.0.1"));
    }

    @Test
    public void testAcceptsIpv6Literal() {
        Assertions.assertDoesNotThrow(() -> Server.requireIpLiteral("0:0:0:0:0:0:0:1"));
        Assertions.assertDoesNotThrow(() -> Server.requireIpLiteral("fe80:0:0:0:0:0:0:1"));
    }

    @Test
    public void testRejectsHostName() {
        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> Server.requireIpLiteral("seata.aaa.com"));
        Assertions.assertTrue(
                ex.getMessage().contains("SEATA_IP must be an IP address literal"),
                "error message should explain the ip literal contract");
        Assertions.assertTrue(
                ex.getMessage().contains("seata.aaa.com"), "error message should contain the rejected input");
    }

    /**
     * A host name that would resolve must be rejected as well, and this
     * check is a pure string validation, so no dns lookup happens here.
     */
    @Test
    public void testRejectsResolvableHostNameWithoutDnsLookup() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.requireIpLiteral("localhost"));
    }

    @Test
    public void testRejectsArbitraryInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.requireIpLiteral("abc"));
    }
}
