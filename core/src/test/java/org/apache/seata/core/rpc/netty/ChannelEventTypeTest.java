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
package org.apache.seata.core.rpc.netty;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChannelEventTypeTest {

    @Test
    public void testValues() {
        ChannelEventType[] types = ChannelEventType.values();
        Assertions.assertEquals(4, types.length);
        Assertions.assertEquals(ChannelEventType.CONNECTED, types[0]);
        Assertions.assertEquals(ChannelEventType.DISCONNECTED, types[1]);
        Assertions.assertEquals(ChannelEventType.EXCEPTION, types[2]);
        Assertions.assertEquals(ChannelEventType.IDLE, types[3]);
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(ChannelEventType.CONNECTED, ChannelEventType.valueOf("CONNECTED"));
        Assertions.assertEquals(ChannelEventType.DISCONNECTED, ChannelEventType.valueOf("DISCONNECTED"));
        Assertions.assertEquals(ChannelEventType.EXCEPTION, ChannelEventType.valueOf("EXCEPTION"));
        Assertions.assertEquals(ChannelEventType.IDLE, ChannelEventType.valueOf("IDLE"));
    }

    @Test
    public void testOrdinal() {
        Assertions.assertEquals(0, ChannelEventType.CONNECTED.ordinal());
        Assertions.assertEquals(1, ChannelEventType.DISCONNECTED.ordinal());
        Assertions.assertEquals(2, ChannelEventType.EXCEPTION.ordinal());
        Assertions.assertEquals(3, ChannelEventType.IDLE.ordinal());
    }

    @Test
    public void testEnumIdentity() {
        Assertions.assertSame(ChannelEventType.CONNECTED, ChannelEventType.valueOf("CONNECTED"));
        Assertions.assertSame(ChannelEventType.DISCONNECTED, ChannelEventType.valueOf("DISCONNECTED"));
    }
}
