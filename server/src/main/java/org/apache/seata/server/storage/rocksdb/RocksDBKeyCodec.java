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

import org.apache.seata.core.model.GlobalStatus;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Stable binary key codec for RocksDB file store engine.
 */
public final class RocksDBKeyCodec {

    private static final int LONG_BYTE_SIZE = Long.BYTES;
    private static final int INT_BYTE_SIZE = Integer.BYTES;

    private RocksDBKeyCodec() {}

    public static byte[] encodeXid(String xid) {
        return encodeComponent(xid);
    }

    public static byte[] encodeXidPrefix(String xid) {
        return encodeComponent(xid);
    }

    public static byte[] encodeBranch(String xid, long branchId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, encodeComponent(xid));
        writeLong(out, branchId);
        return out.toByteArray();
    }

    public static byte[] encodeBranchPrefix(String xid, long branchId) {
        return encodeBranch(xid, branchId);
    }

    public static byte[] encodeRowLock(String resourceId, String tableName, String pk) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, encodeComponent(resourceId));
        write(out, encodeComponent(tableName));
        write(out, encodeComponent(pk));
        return out.toByteArray();
    }

    public static byte[] encodeLockBranchIndex(String xid, long branchId, byte[] lockKey) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, encodeBranch(xid, branchId));
        write(out, encodeComponent(lockKey));
        return out.toByteArray();
    }

    public static byte[] encodeLockBranchIndexBranchPrefix(String xid, long branchId) {
        return encodeBranch(xid, branchId);
    }

    public static byte[] encodeLockBranchIndexGlobalPrefix(String xid) {
        return encodeXidPrefix(xid);
    }

    public static byte[] encodeGlobalStatusIndex(GlobalStatus status, long beginTime, String xid) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, status.getCode());
        writeLong(out, beginTime);
        write(out, encodeComponent(xid));
        return out.toByteArray();
    }

    public static byte[] encodeGlobalStatusPrefix(GlobalStatus status) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, status.getCode());
        return out.toByteArray();
    }

    public static byte[] encodeTransactionIdIndex(long transactionId) {
        return ByteBuffer.allocate(LONG_BYTE_SIZE).putLong(transactionId).array();
    }

    public static boolean startsWith(byte[] key, byte[] prefix) {
        if (key == null || prefix == null || key.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (key[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] prefixEnd(byte[] prefix) {
        if (prefix == null || prefix.length == 0) {
            return null;
        }
        byte[] end = Arrays.copyOf(prefix, prefix.length);
        for (int i = end.length - 1; i >= 0; i--) {
            int value = end[i] & 0xff;
            if (value != 0xff) {
                end[i] = (byte) (value + 1);
                return Arrays.copyOf(end, i + 1);
            }
        }
        return null;
    }

    /**
     * Extract xid from a global status index key.
     *
     * <p>Status index key layout: {@code status_code(4) | beginTime(8) | xid_component(4 + xidBytes)}.
     */
    public static String extractXidFromStatusIndexKey(byte[] key) {
        if (key == null || key.length < INT_BYTE_SIZE + LONG_BYTE_SIZE + INT_BYTE_SIZE) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(key);
        buffer.getInt();
        buffer.getLong();
        int xidLength = buffer.getInt();
        if (xidLength < 0 || buffer.remaining() < xidLength) {
            return null;
        }
        byte[] xidBytes = new byte[xidLength];
        buffer.get(xidBytes);
        return new String(xidBytes, StandardCharsets.UTF_8);
    }

    /**
     * Extract begin time from a global status index key.
     */
    public static long extractBeginTimeFromStatusIndexKey(byte[] key) {
        if (key == null || key.length < INT_BYTE_SIZE + LONG_BYTE_SIZE + INT_BYTE_SIZE) {
            return -1;
        }
        ByteBuffer buffer = ByteBuffer.wrap(key);
        buffer.getInt();
        return buffer.getLong();
    }

    /**
     * Extract status code from a global status index key.
     */
    public static int extractStatusCodeFromStatusIndexKey(byte[] key) {
        if (key == null || key.length < INT_BYTE_SIZE) {
            return -1;
        }
        return ByteBuffer.wrap(key).getInt();
    }

    /**
     * Extract xid from a branch session key.
     *
     * <p>Branch key layout: {@code xid_component(4 + xidBytes) | branchId(8)}.
     */
    public static String extractXidFromBranchKey(byte[] key) {
        if (key == null || key.length < INT_BYTE_SIZE) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(key);
        int xidLength = buffer.getInt();
        if (xidLength < 0 || buffer.remaining() < xidLength + LONG_BYTE_SIZE) {
            return null;
        }
        byte[] xidBytes = new byte[xidLength];
        buffer.get(xidBytes);
        return new String(xidBytes, StandardCharsets.UTF_8);
    }

    private static byte[] encodeComponent(String value) {
        byte[] valueBytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        return encodeComponent(valueBytes);
    }

    private static byte[] encodeComponent(byte[] value) {
        byte[] valueBytes = value == null ? new byte[0] : value;
        ByteBuffer buffer = ByteBuffer.allocate(INT_BYTE_SIZE + valueBytes.length);
        buffer.putInt(valueBytes.length);
        buffer.put(valueBytes);
        return buffer.array();
    }

    private static void writeLong(ByteArrayOutputStream out, long value) {
        write(out, ByteBuffer.allocate(LONG_BYTE_SIZE).putLong(value).array());
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        write(out, ByteBuffer.allocate(INT_BYTE_SIZE).putInt(value).array());
    }

    private static void write(ByteArrayOutputStream out, byte[] bytes) {
        out.write(bytes, 0, bytes.length);
    }
}
