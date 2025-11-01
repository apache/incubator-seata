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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SerializerTypeTest {

    @Test
    public void testGetByCode() {
        Assertions.assertEquals(SerializerType.SEATA, SerializerType.getByCode(1));
        Assertions.assertEquals(SerializerType.PROTOBUF, SerializerType.getByCode(2));
        Assertions.assertEquals(SerializerType.KRYO, SerializerType.getByCode(4));
        Assertions.assertEquals(SerializerType.HESSIAN, SerializerType.getByCode(22));
        Assertions.assertEquals(SerializerType.JACKSON, SerializerType.getByCode(50));
        Assertions.assertEquals(SerializerType.FASTJSON2, SerializerType.getByCode(100));
        Assertions.assertEquals(SerializerType.GRPC, SerializerType.getByCode(40));
        Assertions.assertEquals(SerializerType.FURY, SerializerType.getByCode(86));
    }

    @Test
    public void testGetByCodeWithInvalidCode() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SerializerType.getByCode(999);
        });
    }

    @Test
    public void testGetByCodeWithFSTCode() {
        SerializerType result = SerializerType.getByCode(8);
        Assertions.assertEquals(SerializerType.FST, result);
    }

    @Test
    public void testGetByName() {
        Assertions.assertEquals(SerializerType.SEATA, SerializerType.getByName("SEATA"));
        Assertions.assertEquals(SerializerType.PROTOBUF, SerializerType.getByName("PROTOBUF"));
        Assertions.assertEquals(SerializerType.KRYO, SerializerType.getByName("KRYO"));
        Assertions.assertEquals(SerializerType.HESSIAN, SerializerType.getByName("HESSIAN"));
        Assertions.assertEquals(SerializerType.JACKSON, SerializerType.getByName("JACKSON"));
        Assertions.assertEquals(SerializerType.FASTJSON2, SerializerType.getByName("FASTJSON2"));
        Assertions.assertEquals(SerializerType.GRPC, SerializerType.getByName("GRPC"));
        Assertions.assertEquals(SerializerType.FURY, SerializerType.getByName("FURY"));
    }

    @Test
    public void testGetByNameCaseInsensitive() {
        Assertions.assertEquals(SerializerType.SEATA, SerializerType.getByName("seata"));
        Assertions.assertEquals(SerializerType.PROTOBUF, SerializerType.getByName("protobuf"));
        Assertions.assertEquals(SerializerType.KRYO, SerializerType.getByName("kryo"));
        Assertions.assertEquals(SerializerType.HESSIAN, SerializerType.getByName("hessian"));
    }

    @Test
    public void testGetByNameWithInvalidName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SerializerType.getByName("INVALID");
        });
    }

    @Test
    public void testGetCode() {
        Assertions.assertEquals(1, SerializerType.SEATA.getCode());
        Assertions.assertEquals(2, SerializerType.PROTOBUF.getCode());
        Assertions.assertEquals(4, SerializerType.KRYO.getCode());
        Assertions.assertEquals(8, SerializerType.FST.getCode());
        Assertions.assertEquals(22, SerializerType.HESSIAN.getCode());
        Assertions.assertEquals(50, SerializerType.JACKSON.getCode());
        Assertions.assertEquals(100, SerializerType.FASTJSON2.getCode());
        Assertions.assertEquals(40, SerializerType.GRPC.getCode());
        Assertions.assertEquals(86, SerializerType.FURY.getCode());
    }

    @Test
    public void testAllSerializerTypesHaveUniqueCode() {
        SerializerType[] types = SerializerType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                Assertions.assertNotEquals(
                        types[i].getCode(),
                        types[j].getCode(),
                        "Serializer types " + types[i] + " and " + types[j] + " have the same code");
            }
        }
    }
}
