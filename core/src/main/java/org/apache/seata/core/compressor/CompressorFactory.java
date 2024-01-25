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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * the type compressor factory
 */
public class CompressorFactory {

    /**
     * The constant COMPRESSOR_MAP.
     */
    protected static final Map<CompressorType, Compressor> COMPRESSOR_MAP = new ConcurrentHashMap<>();

    static {
        COMPRESSOR_MAP.put(CompressorType.NONE, new NoneCompressor());
    }

    /**
     * Get compressor by code.
     *
     * @param code the code
     * @return the compressor
     */
    public static Compressor getCompressor(byte code) {
        CompressorType type = CompressorType.getByCode(code);
        return CollectionUtils.computeIfAbsent(COMPRESSOR_MAP, type,
            key -> EnhancedServiceLoader.load(Compressor.class, type.name()));
    }

    /**
     * None compressor
     */
    @LoadLevel(name = "NONE")
    public static class NoneCompressor implements Compressor {
        @Override
        public byte[] compress(byte[] bytes) {
            return bytes;
        }

        @Override
        public byte[] decompress(byte[] bytes) {
            return bytes;
        }
    }

}
