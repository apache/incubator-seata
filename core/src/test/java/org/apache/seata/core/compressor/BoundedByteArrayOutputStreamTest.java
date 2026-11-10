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
package org.apache.seata.core.compressor;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BoundedByteArrayOutputStreamTest {

    @Test
    void checksBeforeWriting() {
        BoundedByteArrayOutputStream out = new BoundedByteArrayOutputStream(3);
        out.write(1);
        out.write(new byte[] {2, 3}, 0, 2);
        assertThrows(IllegalArgumentException.class, () -> out.write(4));
        assertThrows(IllegalArgumentException.class, () -> out.write(new byte[] {4}, 0, 1));
        assertArrayEquals(new byte[] {1, 2, 3}, out.toByteArray());
        out.write(new byte[0], 0, 0);
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(new byte[1], 0, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new BoundedByteArrayOutputStream(-1));
    }

    @Test
    void capsSingleByteGrowthAtNonPowerOfTwoLimit() {
        InspectableStream out = new InspectableStream(8193);
        byte[] bytes = new byte[8193];
        Arrays.fill(bytes, (byte) 7);
        out.write(bytes, 0, 8192);
        out.write(7);
        assertEquals(8193, out.capacity());
        assertArrayEquals(bytes, out.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> out.write(8));
        assertEquals(8193, out.capacity());
        assertArrayEquals(bytes, out.toByteArray());
    }

    @Test
    void capsBulkGrowthAtNonPowerOfTwoLimit() {
        InspectableStream out = new InspectableStream(10000);
        byte[] bytes = new byte[10000];
        Arrays.fill(bytes, (byte) 3);
        out.write(bytes, 0, 8192);
        out.write(bytes, 8192, bytes.length - 8192);
        assertEquals(10000, out.capacity());
        assertArrayEquals(bytes, out.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> out.write(bytes, 0, 1));
        assertEquals(10000, out.capacity());
        assertArrayEquals(bytes, out.toByteArray());
    }

    @Test
    void capsGrowthAfterLargeWrites() {
        int limit = 8 * 1024 * 1024;
        InspectableStream out = new InspectableStream(limit);
        byte[] bytes = new byte[3 * 1024 * 1024];
        Arrays.fill(bytes, (byte) 5);
        out.write(bytes, 0, bytes.length);
        out.write(5);
        out.write(bytes, 0, bytes.length);
        assertEquals(2 * bytes.length + 1, out.size());
        assertTrue(out.capacity() >= out.size());
        assertTrue(out.capacity() <= limit);
        byte[] expected = new byte[out.size()];
        Arrays.fill(expected, (byte) 5);
        assertArrayEquals(expected, out.toByteArray());
    }

    @Test
    void zeroLimitDoesNotAllocateOrAcceptData() {
        InspectableStream out = new InspectableStream(0);
        out.write(new byte[0], 0, 0);
        assertThrows(IllegalArgumentException.class, () -> out.write(1));
        assertThrows(IllegalArgumentException.class, () -> out.write(new byte[1], 0, 1));
        assertEquals(0, out.size());
        assertEquals(0, out.capacity());
    }

    private static class InspectableStream extends BoundedByteArrayOutputStream {
        InspectableStream(int maxOutputSize) {
            super(maxOutputSize);
        }

        int capacity() {
            return buf.length;
        }
    }

    @Test
    void legacyImplementationsMustOptIntoBoundedDecompression() {
        Compressor legacy = new Compressor() {
            public byte[] compress(byte[] bytes) {
                return bytes;
            }

            public byte[] decompress(byte[] bytes) {
                fail("Legacy method must not be called for bounded decompression");
                return bytes;
            }
        };
        assertThrows(UnsupportedOperationException.class, () -> legacy.decompress(new byte[1], 1));
    }
}
