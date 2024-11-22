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
 *
 */
public enum SerializerType {

    /**
     * The seata.
     * <p>
     * Math.pow(2, 0)
     */
    SEATA((byte)0x1),

    /**
     * The protobuf, 'org.apache.seata:seata-serializer-protobuf' dependency must be referenced manually.
     * <p>
     * Math.pow(2, 1)
     */
    PROTOBUF((byte)0x2),

    /**
     * The kryo.
     * <p>
     * Math.pow(2, 2)
     */
    KRYO((byte)0x4),

    /**
     * The fst but it's been removed.
     * <p>
     * Math.pow(2, 3)
     */
    FST((byte)0x8),

    /**
     * The hessian.
     * <p>
     * Math.pow(2, 4)
     */
    HESSIAN((byte)0x16),

    /**
     * The jackson.
     * <p>
     * Math.pow(2, 5)
     */
    JACKSON((byte)0x32),

    /**
     * The fastjson2.
     * <p>
     * Math.pow(2, 6)
     */
    FASTJSON2((byte)0x64),


    /**
     * The grpc
     * <p>
     * Math.pow(2, 7)
     */
    GRPC((byte) 0x128);

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
