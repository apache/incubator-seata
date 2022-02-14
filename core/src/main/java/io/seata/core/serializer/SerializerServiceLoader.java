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
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.common.util.ReflectionUtil;

/**
 * The Service Loader for the interface {@link Serializer}
 *
 * @author wang.liang
 */
public final class SerializerServiceLoader {

    private SerializerServiceLoader() {
    }


    private static final String PROTOBUF_SERIALIZER_CLASS_NAME = "io.seata.serializer.protobuf.ProtobufSerializer";

    /**
     * Load the service of {@link Serializer}
     *
     * @param type the serializer type
     * @return the service of {@link Serializer}
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static Serializer load(SerializerType type) throws EnhancedServiceNotFoundException {
        if (type == SerializerType.PROTOBUF) {
            try {
                ReflectionUtil.getClassByName(PROTOBUF_SERIALIZER_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                throw new EnhancedServiceNotFoundException("'ProtobufSerializer' not found. " +
                        "Please manually reference 'io.seata:seata-serializer-protobuf' dependency ", e);
            }
        }
        return EnhancedServiceLoader.load(Serializer.class, type.name());
    }
}
