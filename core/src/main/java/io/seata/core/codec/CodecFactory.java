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
package io.seata.core.codec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.EnhancedServiceLoader;

/**
 * The type Codec factory.
 *
 * @author zhangsen
 */
public class CodecFactory {

    /**
     * The constant CODEC_MAP.
     */
    protected static final Map<CodecType, Codec> CODEC_MAP = new ConcurrentHashMap<CodecType, Codec>();

    /**
     * Get codec codec.
     *
     * @param codec the code
     * @return the codec
     */
    public static Codec getCodec(byte codec) {
        CodecType codecType = CodecType.getByCode(codec);
        if (CODEC_MAP.get(codecType) != null) {
            return CODEC_MAP.get(codecType);
        }
        Codec codecImpl = EnhancedServiceLoader.load(Codec.class, codecType.name());
        CODEC_MAP.putIfAbsent(codecType, codecImpl);
        return codecImpl;
    }

    /**
     * Encode byte [ ].
     *
     * @param <T>   the type parameter
     * @param codec the codec
     * @param t     the t
     * @return the byte [ ]
     */
    public static <T> byte[] encode(byte codec, T t) {
        return getCodec(codec).encode(t);
    }

    /**
     * Decode t.
     *
     * @param <T>   the type parameter
     * @param codec the code
     * @param bytes the bytes
     * @return the t
     */
    public static <T> T decode(byte codec, byte[] bytes) {
        return getCodec(codec).decode(bytes);
    }

}
