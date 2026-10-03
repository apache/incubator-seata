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
package org.apache.seata.server.storage.rocksdb.index;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Secondary index manager for RocksDB global sessions.
 */
public class RocksDBIndexManager {

    public static final int INDEX_VERSION = 1;
    public static final String INDEX_VERSION_KEY = "index_version";
    public static final String INDEX_BUILD_STATUS_KEY = "index_build_status";
    public static final String INDEX_BUILD_STATUS_IN_PROGRESS = "in_progress";
    public static final String INDEX_BUILD_STATUS_COMPLETED = "completed";

    private static final byte[] EMPTY_PREFIX = new byte[0];

    private final RocksDBStoreEngine storeEngine;

    public RocksDBIndexManager(RocksDBStoreEngine storeEngine) {
        this.storeEngine = storeEngine;
    }

    public void ensureReady() {
        String version = getMetadata(INDEX_VERSION_KEY);
        String status = getMetadata(INDEX_BUILD_STATUS_KEY);
        if (version != null) {
            int parsedVersion = parseIndexVersion(version);
            if (parsedVersion > INDEX_VERSION) {
                throw new StoreException("unsupported RocksDB index version:" + parsedVersion);
            }
        }
        if (Integer.toString(INDEX_VERSION).equals(version) && INDEX_BUILD_STATUS_COMPLETED.equals(status)) {
            return;
        }
        rebuildFromGlobalSessions();
    }

    public void rebuildFromGlobalSessions() {
        putMetadata(INDEX_BUILD_STATUS_KEY, INDEX_BUILD_STATUS_IN_PROGRESS);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, EMPTY_PREFIX);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.TRANSACTION_ID_INDEX, EMPTY_PREFIX);

        WriteBatch[] batch = new WriteBatch[] {new WriteBatch()};
        int[] count = new int[] {0};
        try {
            storeEngine.scanByPrefix(RocksDBColumnFamily.GLOBAL_SESSION, EMPTY_PREFIX, (key, value) -> {
                putGlobalIndexes(batch[0], decodeGlobalSession(value));
                count[0]++;
                if (count[0] >= 1024) {
                    storeEngine.write(batch[0]);
                    batch[0].close();
                    batch[0] = new WriteBatch();
                    count[0] = 0;
                }
            });
            if (count[0] > 0) {
                storeEngine.write(batch[0]);
            }
        } finally {
            batch[0].close();
        }

        try (WriteBatch metadataBatch = new WriteBatch()) {
            metadataBatch.put(
                    storeEngine.handle(RocksDBColumnFamily.METADATA),
                    bytes(INDEX_VERSION_KEY),
                    bytes(Integer.toString(INDEX_VERSION)));
            metadataBatch.put(
                    storeEngine.handle(RocksDBColumnFamily.METADATA),
                    bytes(INDEX_BUILD_STATUS_KEY),
                    bytes(INDEX_BUILD_STATUS_COMPLETED));
            storeEngine.write(metadataBatch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "write RocksDB index metadata failed");
        }
    }

    public void putGlobalIndexes(WriteBatch batch, GlobalSession globalSession) throws RocksDBException {
        byte[] xidValue = bytes(globalSession.getXid());
        batch.put(
                storeEngine.handle(RocksDBColumnFamily.GLOBAL_STATUS_INDEX),
                RocksDBKeyCodec.encodeGlobalStatusIndex(
                        globalSession.getStatus(), globalSession.getBeginTime(), globalSession.getXid()),
                xidValue);
        batch.put(
                storeEngine.handle(RocksDBColumnFamily.TRANSACTION_ID_INDEX),
                RocksDBKeyCodec.encodeTransactionIdIndex(globalSession.getTransactionId()),
                xidValue);
    }

    public void deleteGlobalIndexes(WriteBatch batch, GlobalSession globalSession) throws RocksDBException {
        batch.delete(
                storeEngine.handle(RocksDBColumnFamily.GLOBAL_STATUS_INDEX),
                RocksDBKeyCodec.encodeGlobalStatusIndex(
                        globalSession.getStatus(), globalSession.getBeginTime(), globalSession.getXid()));
        batch.delete(
                storeEngine.handle(RocksDBColumnFamily.TRANSACTION_ID_INDEX),
                RocksDBKeyCodec.encodeTransactionIdIndex(globalSession.getTransactionId()));
    }

    public String findXidByTransactionId(long transactionId) {
        byte[] value = storeEngine.get(
                RocksDBColumnFamily.TRANSACTION_ID_INDEX, RocksDBKeyCodec.encodeTransactionIdIndex(transactionId));
        return value == null ? null : string(value);
    }

    public List<String> scanXidsByStatus(GlobalStatus status) {
        List<String> xids = new ArrayList<>();
        scanXidsByStatus(status, xids::add);
        return xids;
    }

    public void scanXidsByStatus(GlobalStatus status, Consumer<String> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        storeEngine.scanByPrefix(
                RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                RocksDBKeyCodec.encodeGlobalStatusPrefix(status),
                (key, value) -> consumer.accept(string(value)));
    }

    private GlobalSession decodeGlobalSession(byte[] value) {
        RocksDBValueCodec.DecodedValue decodedValue = RocksDBValueCodec.decode(value);
        if (decodedValue.getType() != RocksDBValueCodec.ValueType.GLOBAL_SESSION) {
            throw new StoreException("unexpected RocksDB value type for global session:" + decodedValue.getType());
        }
        GlobalSession globalSession = new GlobalSession(null, null, null, 0, true);
        globalSession.decode(decodedValue.getPayload());
        return globalSession;
    }

    private int parseIndexVersion(String version) {
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            throw new StoreException(e, "invalid RocksDB index version metadata:" + version);
        }
    }

    private String getMetadata(String key) {
        byte[] value = storeEngine.get(RocksDBColumnFamily.METADATA, bytes(key));
        return value == null ? null : string(value);
    }

    private void putMetadata(String key, String value) {
        storeEngine.put(RocksDBColumnFamily.METADATA, bytes(key), bytes(value));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String string(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
