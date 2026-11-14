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
package org.apache.seata.core.protocol;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The server version holder test.
 */
class ServerVersionHolderTest {

    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel();
    }

    @AfterEach
    void tearDown() {
        channel.finishAndReleaseAll();
    }

    @Test
    void putAndGetServerVersionTest() {
        ServerVersionHolder.putServerVersion(channel, "2.1.0");
        assertEquals("2.1.0", ServerVersionHolder.getServerVersion(channel));

        // a re-registration on the same channel overwrites the previous version
        ServerVersionHolder.putServerVersion(channel, "2.6.0");
        assertEquals("2.6.0", ServerVersionHolder.getServerVersion(channel));
    }

    @Test
    void getServerVersionReturnNullWhenNotRegisteredTest() {
        assertNull(ServerVersionHolder.getServerVersion(channel));
        assertNull(ServerVersionHolder.getServerVersion(null));
    }

    @Test
    void putBlankValueIgnoredTest() {
        ServerVersionHolder.putServerVersion(channel, null);
        ServerVersionHolder.putServerVersion(channel, "");
        assertNull(ServerVersionHolder.getServerVersion(channel));

        ServerVersionHolder.putServerVersion(null, "2.6.0");
        assertNull(ServerVersionHolder.getServerVersion(null));
    }

    @Test
    void versionIsBoundToEachChannelTest() {
        // two channels to the same address may reach different TC instances behind a load balancer
        EmbeddedChannel otherChannel = new EmbeddedChannel();
        try {
            ServerVersionHolder.putServerVersion(channel, "2.5.0");
            ServerVersionHolder.putServerVersion(otherChannel, "2.6.0");

            assertEquals("2.5.0", ServerVersionHolder.getServerVersion(channel));
            assertEquals("2.6.0", ServerVersionHolder.getServerVersion(otherChannel));
            assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(channel, Version.VERSION_2_6_0));
            assertTrue(ServerVersionHolder.isServerAboveOrEqualVersion(otherChannel, Version.VERSION_2_6_0));
        } finally {
            otherChannel.finishAndReleaseAll();
        }
    }

    @Test
    void isServerAboveOrEqualVersionTest() {
        ServerVersionHolder.putServerVersion(channel, "2.6.0");
        assertTrue(ServerVersionHolder.isServerAboveOrEqualVersion(channel, Version.VERSION_2_6_0));
        assertTrue(ServerVersionHolder.isServerAboveOrEqualVersion(channel, "2.5.0"));
        assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(channel, "2.7.0"));
    }

    @Test
    void isServerAboveOrEqualVersionReturnFalseWhenTargetVersionIsBlankTest() {
        ServerVersionHolder.putServerVersion(channel, "2.6.0");

        assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(channel, null));
        assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(channel, ""));
        assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(channel, "  "));
    }

    @Test
    void isServerAboveOrEqualVersionReturnFalseWhenUnknownTest() {
        assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(channel, Version.VERSION_2_6_0));
        assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(null, Version.VERSION_2_6_0));
    }

    @Test
    void channelWithoutAttributesIsTreatedAsUnknownTest() {
        Channel mockChannel = mock(Channel.class);

        ServerVersionHolder.putServerVersion(mockChannel, "2.6.0");

        assertNull(ServerVersionHolder.getServerVersion(mockChannel));
        assertFalse(ServerVersionHolder.isServerAboveOrEqualVersion(mockChannel, Version.VERSION_2_6_0));
    }
}
