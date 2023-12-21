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
package io.seata.compressor.lz4;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author diguage
 */

public class Lz4CompressorTest {
    @Test
    public void testCompressAndDecompress() {
        Lz4Compressor compressor = new Lz4Compressor();
        String content = "a0123456789";
        byte[] bytes = content.getBytes();
        bytes = compressor.compress(bytes);
        byte[] result = compressor.decompress(bytes);
        Assertions.assertEquals(new String(result), content);
    }
}