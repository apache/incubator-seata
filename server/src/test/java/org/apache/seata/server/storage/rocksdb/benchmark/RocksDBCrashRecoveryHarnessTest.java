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
package org.apache.seata.server.storage.rocksdb.benchmark;

import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class RocksDBCrashRecoveryHarnessTest {

    @TempDir
    Path tempDir;

    @Test
    void testCleanModeReopensCleanStoreWithAllSessions() throws Exception {
        Path dbPath = tempDir.resolve("clean");

        RocksDBCrashRecoveryHarness.main(
                new String[] {"--mode=clean", "--dbPath=" + dbPath, "--warmupWrites=2", "--count=3", "--syncWrite=true"
                });

        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(new RocksDBStoreConfig(dbPath.toString(), true))) {
            Assertions.assertTrue(engine.wasLastShutdownClean());
            Assertions.assertEquals(
                    5,
                    engine.prefixScan(RocksDBColumnFamily.GLOBAL_SESSION, new byte[0])
                            .size());
        }
    }

    @Test
    void testPeriodicAfterSyncCrashRecovery() throws Exception {
        Path dbPath = tempDir.resolve("periodic");
        Path checkpoint = tempDir.resolve("periodic.checkpoint");

        RocksDBCrashRecoveryHarness.main(new String[] {
            "--mode=parent",
            "--dbPath=" + dbPath,
            "--checkpoint=" + checkpoint,
            "--warmupWrites=1",
            "--count=1",
            "--checkpointAfter=1",
            "--syncWrite=false",
            "--walSyncMode=periodic",
            "--walSyncIntervalMillis=10",
            "--walSyncWriteThreshold=100000",
            "--checkpointPolicy=afterSync",
            "--checkpointTimeoutMillis=30000",
            "--checkpointSyncTimeoutMillis=30000"
        });

        Assertions.assertTrue(Files.exists(checkpoint));
    }
}
