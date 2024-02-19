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
package org.apache.seata.core.serializer;

import java.util.HashSet;
import java.util.Set;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Service Loader for the interface {@link Serializer}
 *
 */
public final class SerializerServiceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializerServiceLoader.class);
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private SerializerServiceLoader() {
    }


    private static final String PROTOBUF_SERIALIZER_CLASS_NAME = "org.apache.seata.serializer.protobuf.ProtobufSerializer";

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
                        "Please manually reference 'org.apache.seata:seata-serializer-protobuf' dependency ", e);
            }
        }
        return EnhancedServiceLoader.load(Serializer.class, type.name());
    }

    public static Set<SerializerType> getSupportedSerializers() {
        Set<SerializerType> supportedSerializers = new HashSet<>();
        String serializerNames = CONFIG.getConfig(ConfigurationKeys.SERIALIZE_FOR_RPC, SerializerType.SEATA.name());
        String[] serializerNameArray = serializerNames.split(",");
        for (String serializerName : serializerNameArray) {
            try {
                SerializerType serializerType = SerializerType.getByName(serializerName);
                if (serializerType != null) {
                    supportedSerializers.add(serializerType);
                }
            } catch (IllegalArgumentException ignore) {
                LOGGER.warn("Invalid serializer name: " + serializerName);
            }
        }
        return supportedSerializers;
    }
}
