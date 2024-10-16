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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.core.serializer.SerializerType.FASTJSON2;
import static org.apache.seata.core.serializer.SerializerType.HESSIAN;
import static org.apache.seata.core.serializer.SerializerType.KRYO;
import static org.apache.seata.core.serializer.SerializerType.PROTOBUF;
import static org.apache.seata.core.serializer.SerializerType.SEATA;

import java.util.HashMap;
import java.util.Map;

/**
 * The Service Loader for the interface {@link Serializer}
 */
public final class SerializerServiceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializerServiceLoader.class);
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final SerializerType[] DEFAULT_SERIALIZER_TYPE = new SerializerType[]{SEATA, PROTOBUF, KRYO, HESSIAN, FASTJSON2};

    private final static Map<String, Serializer> SERIALIZER_MAP = new HashMap<>();

    private static final String SPLIT_CHAR = ",";

    private SerializerServiceLoader() {
    }

    private static final String PROTOBUF_SERIALIZER_CLASS_NAME = "org.apache.seata.serializer.protobuf.ProtobufSerializer";
    private static final boolean CONTAINS_PROTOBUF_DEPENDENCY = ReflectionUtil.isClassPresent(PROTOBUF_SERIALIZER_CLASS_NAME);

    /**
     * Load the service of {@link Serializer}
     *
     * @param type the serializer type
     * @return the service of {@link Serializer}
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static Serializer load(SerializerType type, byte version) throws EnhancedServiceNotFoundException {
        // The following code is only used to kindly prompt users to add missing dependencies.
        if (type == SerializerType.PROTOBUF && !CONTAINS_PROTOBUF_DEPENDENCY) {
            throw new EnhancedServiceNotFoundException("The class '" + PROTOBUF_SERIALIZER_CLASS_NAME + "' not found. " +
                    "Please manually reference 'org.apache.seata:seata-serializer-protobuf' dependency.");
        }

        String key = serializerKey(type, version);
        Serializer serializer = SERIALIZER_MAP.get(key);
        if (serializer == null) {
            if (type == SerializerType.SEATA) {
                serializer = EnhancedServiceLoader.load(Serializer.class, type.name(), new Object[]{version});
            } else {
                serializer = EnhancedServiceLoader.load(Serializer.class, type.name());
            }
            SERIALIZER_MAP.put(key, serializer);
        }
        return serializer;
    }

    /**
     * Load the service of {@link Serializer}
     *
     * @param type the serializer type
     * @return the service of {@link Serializer}
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static Serializer load(SerializerType type) throws EnhancedServiceNotFoundException {
        if (type == SerializerType.PROTOBUF && !CONTAINS_PROTOBUF_DEPENDENCY) {
            throw new EnhancedServiceNotFoundException("The class '" + PROTOBUF_SERIALIZER_CLASS_NAME + "' not found. " +
                "Please manually reference 'org.apache.seata:seata-serializer-protobuf' dependency.");
        }

        String key = type.name();
        Serializer serializer = SERIALIZER_MAP.get(key);
        if (serializer == null) {
            serializer = EnhancedServiceLoader.load(Serializer.class, type.name());

            SERIALIZER_MAP.put(key, serializer);
        }
        return serializer;
    }

    private static String serializerKey(SerializerType type, byte version) {
        if (type == SerializerType.SEATA) {
            return type.name() + version;
        }
        return type.name();
    }


    public static List<SerializerType> getSupportedSerializers() {
        List<SerializerType> supportedSerializers = new ArrayList<>();
        String defaultSupportSerializers = Arrays.stream(DEFAULT_SERIALIZER_TYPE).map(SerializerType::name).collect(Collectors.joining(SPLIT_CHAR));
        String serializerNames = CONFIG.getConfig(ConfigurationKeys.SERIALIZE_FOR_RPC, defaultSupportSerializers);
        String[] serializerNameArray = serializerNames.split(SPLIT_CHAR);
        for (String serializerName : serializerNameArray) {
            try {
                SerializerType serializerType = SerializerType.getByName(serializerName);
                supportedSerializers.add(serializerType);
            } catch (IllegalArgumentException ignore) {
                LOGGER.warn("Invalid serializer name: " + serializerName);
            }
        }
        return supportedSerializers.stream().distinct().collect(Collectors.toList());
    }

    public static SerializerType getDefaultSerializerType() {
        return getSupportedSerializers().get(0);
    }

}