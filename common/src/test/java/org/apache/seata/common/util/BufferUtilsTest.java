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

import java.nio.ByteBuffer;


public class BufferUtilsTest {

    @Test
    public void testFlip() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.flip(byteBuffer));
    }

    @Test
    public void testClear() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.clear(byteBuffer));
    }

    @Test
    public void testLimit() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.limit(byteBuffer, 4));
    }

    @Test
    public void testMark() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.mark(byteBuffer));
    }

    @Test
    public void testPosition() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.position(byteBuffer, 0));
    }

    @Test
    public void testRewind() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.rewind(byteBuffer));
    }

    @Test
    public void testReset() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        BufferUtils.mark(byteBuffer);
        Assertions.assertDoesNotThrow(() -> BufferUtils.reset(byteBuffer));
    }
}
