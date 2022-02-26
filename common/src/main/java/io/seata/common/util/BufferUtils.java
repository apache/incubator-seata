/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.common.util;

import java.nio.Buffer;

/**
 * Explicit cast to {@link Buffer} parent buffer type. It resolves issues with covariant return types in Java 9+ for
 * {@link java.nio.ByteBuffer} and {@link java.nio.CharBuffer}. Explicit casting resolves the NoSuchMethodErrors (e.g
 * java.lang.NoSuchMethodError: java.nio.ByteBuffer.flip(I)Ljava/nio/ByteBuffer) when the project is compiled with
 * newer Java version and run on Java 8.
 * <p/>
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html">Java 8</a> doesn't provide override the
 * following Buffer methods in subclasses:
 *
 * <pre>
 * Buffer clear()
 * Buffer flip()
 * Buffer limit(int newLimit)
 * Buffer mark()
 * Buffer position(int newPosition)
 * Buffer reset()
 * Buffer rewind()
 * </pre>
 *
 * <a href="https://docs.oracle.com/javase/9/docs/api/java/nio/ByteBuffer.html">Java 9</a> introduces the overrides in
 * child classes (e.g the ByteBuffer), but the return type is the specialized one and not the abstract {@link Buffer}.
 * So the code compiled with newer Java is not working on Java 8 unless a workaround with explicit casting is used.
 *
 * @author funkye
 */
public class BufferUtils {

    /**
     * @param buffer byteBuffer
     */
    public static void flip(Buffer buffer) {
        buffer.flip();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void clear(Buffer buffer) {
        buffer.clear();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void limit(Buffer buffer, int newLimit) {
        buffer.limit(newLimit);
    }

    /**
     * @param buffer byteBuffer
     */
    public static void mark(Buffer buffer) {
        buffer.mark();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void position(Buffer buffer, int newPosition) {
        buffer.position(newPosition);
    }

    /**
     * @param buffer byteBuffer
     */
    public static void rewind(Buffer buffer) {
        buffer.rewind();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void reset(Buffer buffer) {
        buffer.reset();
    }

}
