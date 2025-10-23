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

/**
 * The enum serialize type.
 * <p>
 * <b>NOTE: Adding a new serializer type must be non-repeating and within byte range<b/>
 * </p>
 */
public enum SerializerType {

    /**
     * The seata.
     */
    SEATA((byte) 1),

    /**
     * The protobuf, 'org.apache.seata:seata-serializer-protobuf' dependency must be referenced manually.
     */
    PROTOBUF((byte) 2),

    /**
     * The kryo.
     */
    KRYO((byte) 4),

    /**
     * The fst but it's been removed.
     */
    FST((byte) 8),

    /**
     * The hessian.
     */
    HESSIAN((byte) 22),

    /**
     * The jackson.
     */
    JACKSON((byte) 50),

    /**
     * The fastjson2.
     */
    FASTJSON2((byte) 100),

    /**
     * The grpc
     */
    GRPC((byte) 40),

    /**
     * The fury.
     */
    FURY((byte) 86);

    private final byte code;

    SerializerType(final byte code) {
        this.code = code;
    }

    /**
     * Gets result code.
     *
     * @param code the code
     * @return the result code
     */
    public static SerializerType getByCode(int code) {
        for (SerializerType b : SerializerType.values()) {
            if (code == b.code) {
                return b;
            }
        }
        if (code == SerializerType.FST.getCode()) {
            throw new IllegalArgumentException(
                    "Since fst is no longer maintained, this serialization extension has been removed from version 2.0 for security and stability reasons.");
        }
        throw new IllegalArgumentException("unknown codec:" + code);
    }

    /**
     * Gets result code.
     *
     * @param name the name
     * @return the result code
     */
    public static SerializerType getByName(String name) {
        for (SerializerType b : SerializerType.values()) {
            if (b.name().equalsIgnoreCase(name)) {
                return b;
            }
        }
        throw new IllegalArgumentException("unknown codec:" + name);
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public byte getCode() {
        return code;
    }
}
