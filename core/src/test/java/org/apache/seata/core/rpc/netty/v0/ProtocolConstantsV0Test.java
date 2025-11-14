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
package org.apache.seata.core.rpc.netty.v0;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProtocolConstantsV0Test {

    @Test
    public void testMagicConstant() {
        Assertions.assertEquals((short) 0xdada, ProtocolConstantsV0.MAGIC);
    }

    @Test
    public void testHeadLength() {
        Assertions.assertEquals(14, ProtocolConstantsV0.HEAD_LENGTH);
    }

    @Test
    public void testFlagRequest() {
        Assertions.assertEquals(0x80, ProtocolConstantsV0.FLAG_REQUEST);
    }

    @Test
    public void testFlagAsync() {
        Assertions.assertEquals(0x40, ProtocolConstantsV0.FLAG_ASYNC);
    }

    @Test
    public void testFlagHeartbeat() {
        Assertions.assertEquals(0x20, ProtocolConstantsV0.FLAG_HEARTBEAT);
    }

    @Test
    public void testFlagSeataCodec() {
        Assertions.assertEquals(0x10, ProtocolConstantsV0.FLAG_SEATA_CODEC);
    }

    @Test
    public void testFlagValuesAreUnique() {
        short[] flags = new short[] {
            ProtocolConstantsV0.FLAG_REQUEST,
            ProtocolConstantsV0.FLAG_ASYNC,
            ProtocolConstantsV0.FLAG_HEARTBEAT,
            ProtocolConstantsV0.FLAG_SEATA_CODEC
        };

        for (int i = 0; i < flags.length; i++) {
            for (int j = i + 1; j < flags.length; j++) {
                Assertions.assertNotEquals(flags[i], flags[j], "Flag values should be unique");
            }
        }
    }

    @Test
    public void testFlagBitwise() {
        short combined = (short) (ProtocolConstantsV0.FLAG_REQUEST | ProtocolConstantsV0.FLAG_ASYNC);
        Assertions.assertEquals(0xC0, combined);

        boolean hasRequest = (combined & ProtocolConstantsV0.FLAG_REQUEST) != 0;
        boolean hasAsync = (combined & ProtocolConstantsV0.FLAG_ASYNC) != 0;
        boolean hasHeartbeat = (combined & ProtocolConstantsV0.FLAG_HEARTBEAT) != 0;

        Assertions.assertTrue(hasRequest);
        Assertions.assertTrue(hasAsync);
        Assertions.assertFalse(hasHeartbeat);
    }

    @Test
    public void testMagicNumberIsTwoBytes() {
        Assertions.assertTrue(ProtocolConstantsV0.MAGIC >= Short.MIN_VALUE);
        Assertions.assertTrue(ProtocolConstantsV0.MAGIC <= Short.MAX_VALUE);
    }
}
