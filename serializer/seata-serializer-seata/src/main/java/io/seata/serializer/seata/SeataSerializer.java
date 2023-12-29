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
package io.seata.serializer.seata;

import com.google.common.collect.ImmutableMap;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.serializer.Serializer;
import io.seata.serializer.seata.protocol.v0.SeataSerializerV0;
import io.seata.serializer.seata.protocol.v1.SeataSerializerV1;

import java.util.Map;

/**
 * The Seata codec.
 */
@LoadLevel(name = "SEATA", scope = Scope.PROTOTYPE)
public class SeataSerializer implements Serializer {

    Serializer versionSeataSerializer;

    static Map<Byte, Serializer> VERSION_MAP = ImmutableMap.<Byte, Serializer>builder()
            .put(ProtocolConstants.VERSION_0, new SeataSerializerV0())
            .put(ProtocolConstants.VERSION_1, new SeataSerializerV1())
            .build();

    public SeataSerializer(Byte version){
        versionSeataSerializer =  VERSION_MAP.get(version);
        if (versionSeataSerializer == null) {
            throw new IllegalArgumentException("version is not supported");
        }
    }
    @Override
    public <T> byte[] serialize(T t) {
        return versionSeataSerializer.serialize(t);
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        return versionSeataSerializer.deserialize(bytes);
    }

}
