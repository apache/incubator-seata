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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class CompressorFactoryTest {

    @Test
    void testGetCompressorNone() {
        Compressor compressor = CompressorFactory.getCompressor(CompressorType.NONE.getCode());
        assertNotNull(compressor);
        assertTrue(compressor instanceof CompressorFactory.NoneCompressor);
    }

    @Test
    void testNoneCompressor() {
        CompressorFactory.NoneCompressor noneCompressor = new CompressorFactory.NoneCompressor();
        byte[] testData = "Test data".getBytes();
        
        byte[] compressed = noneCompressor.compress(testData);
        assertArrayEquals(testData, compressed);
        
        byte[] decompressed = noneCompressor.decompress(compressed);
        assertArrayEquals(testData, decompressed);
    }

    @Test
    void testCompressorCaching() {
        Compressor compressor1 = CompressorFactory.getCompressor(CompressorType.NONE.getCode());
        Compressor compressor2 = CompressorFactory.getCompressor(CompressorType.NONE.getCode());
        assertSame(compressor1, compressor2);
    }

    @Test
    void testInvalidCompressorCode() {
        assertThrows(IllegalArgumentException.class, () -> CompressorFactory.getCompressor((byte) -1));
    }

    @Test
    void testCompressorMapInitialization() {
        assertTrue(CompressorFactory.COMPRESSOR_MAP.containsKey(CompressorType.NONE));
        assertTrue(CompressorFactory.COMPRESSOR_MAP.get(CompressorType.NONE) instanceof CompressorFactory.NoneCompressor);
    }
}