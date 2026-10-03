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
package org.apache.seata.server.storage.rocksdb;

import org.apache.seata.common.exception.StoreException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class RocksDBValueCodecTest {

    @Test
    void testEncodeDecode() {
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

        RocksDBValueCodec.DecodedValue decoded =
                RocksDBValueCodec.decode(RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.GLOBAL_SESSION, payload));

        Assertions.assertEquals(RocksDBValueCodec.ValueType.GLOBAL_SESSION, decoded.getType());
        Assertions.assertEquals(1, decoded.getVersion());
        Assertions.assertArrayEquals(payload, decoded.getPayload());
    }

    @Test
    void testDecodeRejectsInvalidHeader() {
        Assertions.assertThrows(StoreException.class, () -> RocksDBValueCodec.decode(new byte[] {1, 2, 3}));
    }

    @Test
    void testDecodeRejectsUnknownType() {
        byte[] encoded = RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.METADATA, new byte[0]);
        encoded[6] = 99;

        Assertions.assertThrows(StoreException.class, () -> RocksDBValueCodec.decode(encoded));
    }
}
