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
package org.apache.seata.server.storage.rocksdb;

import org.apache.seata.common.exception.StoreException;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Versioned value codec for RocksDB file store engine.
 */
public final class RocksDBValueCodec {

    private static final int MAGIC = 0x53415244;
    private static final short VERSION = 1;
    private static final int HEADER_SIZE = Integer.BYTES + Short.BYTES + Byte.BYTES;

    private RocksDBValueCodec() {}

    public static byte[] encode(ValueType type, byte[] payload) {
        byte[] value = payload == null ? new byte[0] : payload;
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + value.length);
        buffer.putInt(MAGIC);
        buffer.putShort(VERSION);
        buffer.put(type.getCode());
        buffer.put(value);
        return buffer.array();
    }

    public static DecodedValue decode(byte[] value) {
        if (value == null || value.length < HEADER_SIZE) {
            throw new StoreException("invalid RocksDB value header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new StoreException("invalid RocksDB value magic");
        }
        short version = buffer.getShort();
        if (version != VERSION) {
            throw new StoreException("unsupported RocksDB value version:" + version);
        }
        ValueType type = ValueType.get(buffer.get());
        byte[] payload = Arrays.copyOfRange(value, HEADER_SIZE, value.length);
        return new DecodedValue(version, type, payload);
    }

    /**
     * Encoded value type.
     */
    public enum ValueType {
        /**
         * Global session value.
         */
        GLOBAL_SESSION((byte) 1),
        /**
         * Branch session value.
         */
        BRANCH_SESSION((byte) 2),
        /**
         * Lock holder value.
         */
        LOCK_HOLDER((byte) 3),
        /**
         * Metadata value.
         */
        METADATA((byte) 4);

        private final byte code;

        ValueType(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        public static ValueType get(byte code) {
            for (ValueType valueType : values()) {
                if (valueType.getCode() == code) {
                    return valueType;
                }
            }
            throw new StoreException("unknown RocksDB value type:" + code);
        }
    }

    /**
     * Decoded RocksDB value.
     */
    public static class DecodedValue {
        private final short version;
        private final ValueType type;
        private final byte[] payload;

        private DecodedValue(short version, ValueType type, byte[] payload) {
            this.version = version;
            this.type = type;
            this.payload = payload;
        }

        public short getVersion() {
            return version;
        }

        public ValueType getType() {
            return type;
        }

        public byte[] getPayload() {
            return Arrays.copyOf(payload, payload.length);
        }
    }
}
