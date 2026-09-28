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

public interface Compressor {

    /**
     * compress byte[] to byte[].
     * @param bytes the bytes
     * @return the byte[]
     */
    byte[] compress(byte[] bytes);

    /**
     * decompress byte[] to byte[].
     * @param bytes the bytes
     * @return the byte[]
     */
    byte[] decompress(byte[] bytes);

    /**
     * Decompress while limiting the number of output bytes.
     * Implementations must enforce the limit before growing the output buffer.
     *
     * @param bytes the compressed bytes
     * @param maxOutputSize the maximum output size, in bytes
     * @return the decompressed bytes
     * @throws UnsupportedOperationException if bounded decompression is not implemented
     */
    default byte[] decompress(byte[] bytes, int maxOutputSize) {
        throw new UnsupportedOperationException("Bounded decompression is not supported");
    }
}
