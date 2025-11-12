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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
class UUIDGeneratorTest {

    @Test
    void generateUUID() {
        Assertions.assertTrue(UUIDGenerator.generateUUID() > 0);
    }

    @Test
    void testMultipleUUIDGeneration() {
        long uuid1 = UUIDGenerator.generateUUID();
        long uuid2 = UUIDGenerator.generateUUID();
        long uuid3 = UUIDGenerator.generateUUID();

        // All UUIDs should be positive
        Assertions.assertTrue(uuid1 > 0);
        Assertions.assertTrue(uuid2 > 0);
        Assertions.assertTrue(uuid3 > 0);

        // All UUIDs should be different
        Assertions.assertNotEquals(uuid1, uuid2);
        Assertions.assertNotEquals(uuid1, uuid3);
        Assertions.assertNotEquals(uuid2, uuid3);
    }

    @Test
    void testInitWithServerNode() {
        // Test initializing with a specific server node
        UUIDGenerator.init(1L);
        long uuid = UUIDGenerator.generateUUID();
        Assertions.assertTrue(uuid > 0);

        // Test initializing with null (auto-generated server node)
        UUIDGenerator.init(null);
        long uuid2 = UUIDGenerator.generateUUID();
        Assertions.assertTrue(uuid2 > 0);
    }

    @Test
    void testInitWithInvalidServerNode() {
        // Test initializing with invalid server node values
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            UUIDGenerator.init(-1L);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            UUIDGenerator.init(1024L); // Max valid value is 1023
        });
    }
}
