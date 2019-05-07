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
 * @data 2019 /5/6
 */
public class CodecFactory {

    /**
     * The constant CODEC_MAP.
     */
    protected static final Map<CodecType, Codec> CODEC_MAP = new ConcurrentHashMap<CodecType, Codec>();

    /**
     * Get codec codec.
     *
     * @param code the code
     * @return the codec
     */
    public static synchronized Codec getCodec(byte code) {
        CodecType codecType = CodecType.getResultCode(code);
        if (CODEC_MAP.get(codecType) != null) {
            return CODEC_MAP.get(codecType);
        }
        Codec codec = EnhancedServiceLoader.load(Codec.class, codecType.name());
        CODEC_MAP.put(codecType, codec);
        return codec;
    }

    /**
     * Encode byte [ ].
     *
     * @param <T>  the type parameter
     * @param code the code
     * @param t    the t
     * @return the byte [ ]
     */
    public static <T> byte[] encode(byte code, T t) {
        return getCodec(code).encode(t);
    }

    /**
     * Decode t.
     *
     * @param <T>   the type parameter
     * @param code  the code
     * @param bytes the bytes
     * @return the t
     */
    public static <T> T decode(byte code, String clazz, byte[] bytes) {
        return getCodec(code).decode(clazz,bytes);
    }

}
