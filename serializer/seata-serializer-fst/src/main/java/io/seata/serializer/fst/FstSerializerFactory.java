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
package io.seata.serializer.fst;

import io.seata.core.serializer.SerializerSecurityRegistry;
import org.nustaq.serialization.FSTConfiguration;

/**
 * @author funkye
 */
public class FstSerializerFactory {

    private static final FstSerializerFactory FACTORY = new FstSerializerFactory();

    private final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();


    public static FstSerializerFactory getDefaultFactory() {
        return FACTORY;
    }

    public FstSerializerFactory() {
        SerializerSecurityRegistry.getAllowClassType().forEach(conf::registerClass);
    }

    public <T> byte[] serialize(T t) {
        return conf.asByteArray(t);
    }

    public <T> T deserialize(byte[] bytes) {
        return (T)conf.asObject(bytes);
    }

}
