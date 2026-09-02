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
package org.apache.seata.server.storage.rocksdb.maintenance;

import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Persisted cursor for the resumable lock-branch index repair.
 */
final class RocksDBLockIndexRepairProgress {

    enum State {
        PAUSED,
        STOPPED
    }

    private static final int MAGIC = 0x524c5231;

    final State state;
    final String runId;
    final byte[] cursor;
    final int deleted;

    RocksDBLockIndexRepairProgress(State state, String runId, byte[] cursor, int deleted) {
        this.state = state;
        this.runId = runId;
        this.cursor = cursor == null ? null : Arrays.copyOf(cursor, cursor.length);
        this.deleted = deleted;
    }

    byte[] encode() {
        byte[] runIdBytes = runId.getBytes(StandardCharsets.UTF_8);
        int cursorLength = cursor == null ? 0 : cursor.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 4 + runIdBytes.length + 4 + cursorLength + 4);
        buffer.putInt(MAGIC);
        buffer.put((byte) state.ordinal());
        buffer.putInt(runIdBytes.length);
        buffer.put(runIdBytes);
        buffer.putInt(cursorLength);
        if (cursorLength > 0) {
            buffer.put(cursor);
        }
        buffer.putInt(deleted);
        return buffer.array();
    }

    static RocksDBLockIndexRepairProgress decode(byte[] value) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            if (buffer.getInt() != MAGIC) {
                throw new IllegalArgumentException("unexpected progress magic");
            }
            int stateOrdinal = buffer.get();
            int runIdLength = buffer.getInt();
            if (runIdLength <= 0 || runIdLength > buffer.remaining()) {
                throw new IllegalArgumentException("invalid repair run id");
            }
            byte[] runIdBytes = new byte[runIdLength];
            buffer.get(runIdBytes);
            int cursorLength = buffer.getInt();
            if (cursorLength < 0 || cursorLength > buffer.remaining() - Integer.BYTES) {
                throw new IllegalArgumentException("invalid repair cursor");
            }
            byte[] cursor = cursorLength == 0 ? null : new byte[cursorLength];
            if (cursor != null) {
                buffer.get(cursor);
            }
            int deleted = buffer.getInt();
            if (buffer.hasRemaining()
                    || stateOrdinal < 0
                    || stateOrdinal >= State.values().length
                    || deleted < 0
                    || (cursor != null && !RocksDBKeyCodec.isValidLockBranchIndexSeekKey(cursor))) {
                throw new IllegalArgumentException("invalid repair progress payload");
            }
            return new RocksDBLockIndexRepairProgress(
                    State.values()[stateOrdinal], new String(runIdBytes, StandardCharsets.UTF_8), cursor, deleted);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid lock index repair progress", e);
        }
    }
}
