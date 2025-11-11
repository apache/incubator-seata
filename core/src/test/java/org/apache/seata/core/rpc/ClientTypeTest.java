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
package org.apache.seata.core.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClientTypeTest {

    @Test
    public void testGetByByteOrdinal() {
        Assertions.assertEquals(ClientType.TM, ClientType.get((byte) 0));
        Assertions.assertEquals(ClientType.RM, ClientType.get((byte) 1));
    }

    @Test
    public void testGetByIntOrdinal() {
        Assertions.assertEquals(ClientType.TM, ClientType.get(0));
        Assertions.assertEquals(ClientType.RM, ClientType.get(1));
    }

    @Test
    public void testGetWithInvalidOrdinal() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ClientType.get(2);
        });
    }

    @Test
    public void testGetWithNegativeOrdinal() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ClientType.get(-1);
        });
    }

    @Test
    public void testValues() {
        ClientType[] types = ClientType.values();
        Assertions.assertEquals(2, types.length);
        Assertions.assertEquals(ClientType.TM, types[0]);
        Assertions.assertEquals(ClientType.RM, types[1]);
    }

    @Test
    public void testOrdinal() {
        Assertions.assertEquals(0, ClientType.TM.ordinal());
        Assertions.assertEquals(1, ClientType.RM.ordinal());
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(ClientType.TM, ClientType.valueOf("TM"));
        Assertions.assertEquals(ClientType.RM, ClientType.valueOf("RM"));
    }
}
