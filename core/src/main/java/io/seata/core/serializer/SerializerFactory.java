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
package io.seata.core.serializer;

import io.seata.common.loader.EnhancedServiceLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Codec factory.
 *
 * @author zhangsen
 */
public class SerializerFactory {

    /**
     * The constant CODEC_MAP.
     */
    protected static final Map<SerializerType, Serializer> CODEC_MAP = new ConcurrentHashMap<SerializerType, Serializer>();

    /**
     * Get serializeCode serializeCode.
     *
     * @param serializeCode the code
     * @return the serializeCode
     */
    public static Serializer getSerializer(byte serializeCode) {
        SerializerType serializerType = SerializerType.getByCode(serializeCode);
        if (CODEC_MAP.get(serializerType) != null) {
            return CODEC_MAP.get(serializerType);
        }
        Serializer codecImpl = EnhancedServiceLoader.load(Serializer.class, serializerType.name());
        CODEC_MAP.putIfAbsent(serializerType, codecImpl);
        return codecImpl;
    }

    /**
     * Encode byte [ ].
     *
     * @param <T>   the type parameter
     * @param serializeCode the serializeCode
     * @param t     the t
     * @return the byte [ ]
     */
    public static <T> byte[] encode(byte serializeCode, T t) {
        return getSerializer(serializeCode).serialize(t);
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
        return getSerializer(codec).deserialize(bytes);
    }

}
