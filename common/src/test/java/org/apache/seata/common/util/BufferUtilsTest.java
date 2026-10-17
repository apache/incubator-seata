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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.InvalidMarkException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BufferUtilsTest {

    static Stream<Arguments> buffers() {
        return Stream.of(
                Arguments.of("heap ByteBuffer", ByteBuffer.allocate(8)),
                Arguments.of("direct ByteBuffer", ByteBuffer.allocateDirect(8)),
                Arguments.of("CharBuffer", CharBuffer.allocate(8)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testFlip(String name, Buffer buffer) {
        buffer.limit(6).position(3).mark();

        BufferUtils.flip(buffer);

        assertState(buffer, 0, 3);
        assertThrows(InvalidMarkException.class, buffer::reset);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testClear(String name, Buffer buffer) {
        buffer.limit(6).position(3).mark();

        BufferUtils.clear(buffer);

        assertState(buffer, 0, 8);
        assertThrows(InvalidMarkException.class, buffer::reset);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testLimit(String name, Buffer buffer) {
        buffer.limit(6).position(5).mark();

        BufferUtils.limit(buffer, 3);

        assertState(buffer, 3, 3);
        assertThrows(InvalidMarkException.class, buffer::reset);

        buffer.position(2).mark().position(3);
        BufferUtils.limit(buffer, 8);

        assertState(buffer, 3, 8);
        buffer.reset();
        assertState(buffer, 2, 8);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testMark(String name, Buffer buffer) {
        buffer.limit(6).position(2).mark().position(3);

        BufferUtils.mark(buffer);

        assertState(buffer, 3, 6);
        buffer.position(5).reset();
        assertState(buffer, 3, 6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testPosition(String name, Buffer buffer) {
        buffer.limit(6).position(2).mark();

        BufferUtils.position(buffer, 5);

        assertState(buffer, 5, 6);
        buffer.reset();
        assertState(buffer, 2, 6);

        BufferUtils.position(buffer, 1);

        assertState(buffer, 1, 6);
        assertThrows(InvalidMarkException.class, buffer::reset);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testRewind(String name, Buffer buffer) {
        buffer.limit(6).position(3).mark();

        BufferUtils.rewind(buffer);

        assertState(buffer, 0, 6);
        assertThrows(InvalidMarkException.class, buffer::reset);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testReset(String name, Buffer buffer) {
        buffer.limit(6).position(2).mark().position(5);

        BufferUtils.reset(buffer);

        assertState(buffer, 2, 6);
        buffer.position(4);
        BufferUtils.reset(buffer);
        assertState(buffer, 2, 6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testLimitRejectsOutOfBoundsValues(String name, Buffer buffer) {
        buffer.limit(6).position(2).mark().position(3);

        assertThrows(IllegalArgumentException.class, () -> BufferUtils.limit(buffer, -1));
        assertState(buffer, 3, 6);
        assertThrows(IllegalArgumentException.class, () -> BufferUtils.limit(buffer, 9));
        assertState(buffer, 3, 6);
        buffer.reset();
        assertState(buffer, 2, 6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testPositionRejectsOutOfBoundsValues(String name, Buffer buffer) {
        buffer.limit(6).position(2).mark().position(3);

        assertThrows(IllegalArgumentException.class, () -> BufferUtils.position(buffer, -1));
        assertState(buffer, 3, 6);
        assertThrows(IllegalArgumentException.class, () -> BufferUtils.position(buffer, 7));
        assertState(buffer, 3, 6);
        buffer.reset();
        assertState(buffer, 2, 6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    public void testResetWithoutMark(String name, Buffer buffer) {
        buffer.limit(6).position(3);

        assertThrows(InvalidMarkException.class, () -> BufferUtils.reset(buffer));
        assertState(buffer, 3, 6);
    }

    private void assertState(Buffer buffer, int position, int limit) {
        assertEquals(position, buffer.position(), "position");
        assertEquals(limit, buffer.limit(), "limit");
        assertEquals(8, buffer.capacity(), "capacity");
    }
}
