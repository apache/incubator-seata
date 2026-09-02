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
package org.apache.seata.server.storage.file.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.BufferUtils;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.store.StoreConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Replays file-mode session logs without starting the file store writer.
 */
public class FileSessionLogReplayer {

    private static final String HISTORY_FILE_POSTFIX = ".1";
    private static final String MIGRATION_MARKER_POSTFIX = ".rocksdb_migrated";
    private static final int MARK_SIZE = Integer.BYTES;

    public boolean hasSessionLogs(Path currentLogPath) {
        return hasReadableData(historyLogPath(currentLogPath)) || hasReadableData(currentLogPath);
    }

    public boolean hasMigrationMarker(Path currentLogPath) {
        return Files.isRegularFile(migrationMarkerPath(currentLogPath));
    }

    public void markMigrated(Path currentLogPath) {
        Path markerPath = migrationMarkerPath(currentLogPath);
        try {
            if (markerPath.getParent() != null) {
                Files.createDirectories(markerPath.getParent());
            }
            Files.write(
                    markerPath,
                    "migrated to rocksdb\n".getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new StoreException(e, "mark file session log migrated failed, marker:" + markerPath);
        }
    }

    public int replay(Path currentLogPath, TransactionWriteStoreVisitor visitor) {
        int count = replayFile(historyLogPath(currentLogPath), visitor);
        return count + replayFile(currentLogPath, visitor);
    }

    private int replayFile(Path logPath, TransactionWriteStoreVisitor visitor) {
        if (!hasReadableData(logPath)) {
            return 0;
        }
        int count = 0;
        try (FileChannel fileChannel = FileChannel.open(logPath, StandardOpenOption.READ)) {
            ByteBuffer sizeBuffer = ByteBuffer.allocate(MARK_SIZE);
            while (fileChannel.position() < fileChannel.size()) {
                BufferUtils.clear(sizeBuffer);
                if (!readFully(fileChannel, sizeBuffer)) {
                    break;
                }
                BufferUtils.flip(sizeBuffer);
                int bodySize = sizeBuffer.getInt();
                if (bodySize <= 0) {
                    throw new StoreException("invalid file session log body size:" + bodySize + ", file:" + logPath);
                }
                if (bodySize > maxReplayBodySize()) {
                    throw new StoreException("file session log body size exceeds limit:" + bodySize + ", max:"
                            + maxReplayBodySize() + ", file:" + logPath);
                }

                ByteBuffer bodyBuffer = ByteBuffer.allocate(bodySize);
                if (!readFully(fileChannel, bodyBuffer)) {
                    break;
                }
                TransactionWriteStore writeStore = new TransactionWriteStore();
                writeStore.decode(bodyBuffer.array());
                visitor.visit(writeStore);
                count++;
            }
            return count;
        } catch (StoreException e) {
            throw e;
        } catch (Exception e) {
            throw new StoreException(e, "replay file session log failed, file:" + logPath);
        }
    }

    private boolean hasReadableData(Path logPath) {
        try {
            return Files.isRegularFile(logPath) && Files.size(logPath) > 0;
        } catch (IOException e) {
            throw new StoreException(e, "inspect file session log failed, file:" + logPath);
        }
    }

    private Path historyLogPath(Path currentLogPath) {
        return currentLogPath.resolveSibling(currentLogPath.getFileName() + HISTORY_FILE_POSTFIX);
    }

    private Path migrationMarkerPath(Path currentLogPath) {
        return currentLogPath.resolveSibling(currentLogPath.getFileName() + MIGRATION_MARKER_POSTFIX);
    }

    private int maxReplayBodySize() {
        return Math.max(StoreConfig.getMaxGlobalSessionSize(), StoreConfig.getMaxBranchSessionSize()) + 1;
    }

    private boolean readFully(FileChannel fileChannel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = fileChannel.read(buffer);
            if (read < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Visitor for decoded file-mode transaction write stores.
     */
    @FunctionalInterface
    public interface TransactionWriteStoreVisitor {
        void visit(TransactionWriteStore writeStore);
    }
}
