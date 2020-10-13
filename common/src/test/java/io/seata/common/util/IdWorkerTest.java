/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IdWorkerTest {

    @Test
    void testNegativeWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new IdWorker(-1L);
        }, "should throw IllegalArgumentException when workerId is negative");
    }

    @Test
    void testTooLargeWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new IdWorker(1024L);
        }, "should throw IllegalArgumentException when workerId is bigger than 1023");
    }

    @Test
    void testNextId() {
        IdWorker worker = new IdWorker(null);
        long id1 = worker.nextId();
        long id2 = worker.nextId();
        assertEquals(1L, id2 - id1, "increment step should be 1");
    }
}