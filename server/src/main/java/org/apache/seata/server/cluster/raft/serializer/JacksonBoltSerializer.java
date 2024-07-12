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
package org.apache.seata.server.cluster.raft.serializer;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.Serializer;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

public class JacksonBoltSerializer implements Serializer {

    private final org.apache.seata.core.serializer.Serializer seataSerializer =
        EnhancedServiceLoader.load(org.apache.seata.core.serializer.Serializer.class,
            SerializerType.getByCode(SerializerType.JACKSON.getCode()).name());

    @Override
    public byte[] serialize(Object obj) throws CodecException {
        try {
            return seataSerializer.serialize(obj);
        } catch (Exception e) {
            throw new CodecException("Failed to serialize data", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, String classOfT) throws CodecException {
        try {
            return seataSerializer.deserialize(data);
        } catch (Exception e) {
            throw new CodecException("Failed to deserialize data", e);
        }
    }

}
