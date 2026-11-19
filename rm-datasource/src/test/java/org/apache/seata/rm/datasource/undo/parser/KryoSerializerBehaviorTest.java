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
package org.apache.seata.rm.datasource.undo.parser;

import org.junit.jupiter.api.Test;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class KryoSerializerBehaviorTest {
    @Test
    void roundTripsLobsAndTimestampPrecision() throws Exception {
        KryoSerializerFactory factory = KryoSerializerFactory.getInstance();
        KryoSerializer serializer = factory.get();
        try {
            SerialBlob blob = serializer.deserialize(serializer.serialize(new SerialBlob(new byte[] {0, 1, -1})));
            assertArrayEquals(new byte[] {0, 1, -1}, blob.getBytes(1, 3));
            SerialClob clob = serializer.deserialize(serializer.serialize(new SerialClob("文本 and text".toCharArray())));
            assertEquals("文本 and text", clob.getSubString(1, (int) clob.length()));
            Timestamp time = Timestamp.valueOf("2024-03-02 12:34:56.123456789");
            Timestamp decoded = serializer.deserialize(serializer.serialize(time));
            assertEquals(time, decoded);
            assertEquals(123456789, decoded.getNanos());
        } finally {
            factory.returnKryo(serializer);
        }
        assertThrows(IllegalArgumentException.class, () -> factory.returnKryo(null));
    }
}
