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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ProtocolConstants}.
 */
public class ProtocolConstantsTest {

    @Test
    public void testMagicCodeBytes() {
        byte[] expected = {(byte) 0xda, (byte) 0xda};
        assertArrayEquals(expected, ProtocolConstants.MAGIC_CODE_BYTES);
    }

    @Test
    public void testVersionConstants() {
        assertEquals(0, ProtocolConstants.VERSION_0);
        assertEquals(1, ProtocolConstants.VERSION_1);
        assertEquals(ProtocolConstants.VERSION_1, ProtocolConstants.VERSION);
    }

    @Test
    public void testMaxFrameLength() {
        assertEquals(8 * 1024 * 1024, ProtocolConstants.MAX_FRAME_LENGTH);
    }

    @Test
    public void testV1HeadLength() {
        assertEquals(16, ProtocolConstants.V1_HEAD_LENGTH);
    }

    @Test
    public void testMessageTypeConstants() {
        assertEquals(0, ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        assertEquals(1, ProtocolConstants.MSGTYPE_RESPONSE);
        assertEquals(2, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        assertEquals(3, ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        assertEquals(4, ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE);
    }

    @Test
    public void testConfiguredCodecIsValid() {
        assertTrue(ProtocolConstants.CONFIGURED_CODEC >= 0);
    }

    @Test
    public void testConfiguredCompressorIsValid() {
        assertTrue(ProtocolConstants.CONFIGURED_COMPRESSOR >= 0);
    }
}
