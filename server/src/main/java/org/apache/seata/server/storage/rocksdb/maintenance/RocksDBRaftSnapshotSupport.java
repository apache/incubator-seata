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

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Raft snapshot preparation support for RocksDB file store engine.
 *
 * <p>This class bridges the existing {@link RocksDBMaintenanceService} checkpoint capability
 * with the future Raft snapshot integration. Phase 4 only guarantees that a RocksDB checkpoint
 * can be created, validated and used to replace a local database directory. Full JRaft
 * {@code StoreSnapshotFile} integration is deferred to a later phase.
 *
 * <p>Intended future Raft flow:
 * <ol>
 *   <li>snapshot save &rarr; create a RocksDB checkpoint via {@link #saveSnapshot(Path, boolean)}</li>
 *   <li>transfer checkpoint files (CURRENT, MANIFEST, OPTIONS, SST, WAL, metadata) to peers</li>
 *   <li>snapshot load &rarr; validate metadata via {@link #validateSnapshotCompatibility(Path)},
 *       close local engine, replace local DB directory via {@link #replaceLocalDbFromSnapshot(Path, Path)},
 *       reopen engine</li>
 * </ol>
 *
 * <p>This class is stateless and safe to instantiate on demand.
 */
public class RocksDBRaftSnapshotSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBRaftSnapshotSupport.class);
    private static final String SNAPSHOT_METADATA_FILE = "seata-checkpoint-metadata.txt";

    /**
     * Runtime-only files that should not be copied during snapshot replace.
     * The LOCK file is held by the running RocksDB process and is recreated on open.
     */
    private static final java.util.Set<String> SKIP_FILES = new java.util.HashSet<>(java.util.Arrays.asList("LOCK"));

    private final RocksDBMaintenanceService maintenanceService;

    public RocksDBRaftSnapshotSupport(RocksDBStoreEngine storeEngine) {
        this.maintenanceService = new RocksDBMaintenanceService(storeEngine);
    }

    /**
     * Create a snapshot of the current RocksDB state at the given directory.
     *
     * <p>Delegates to {@link RocksDBMaintenanceService#createCheckpoint(Path, boolean)}.
     * The snapshot directory will contain all RocksDB files plus a metadata file describing
     * the engine, format version, column families and RocksDB JNI version.
     *
     * @param snapshotDir target directory for the snapshot (must not exist or be empty)
     * @param flush       whether to flush memtables before creating the snapshot
     */
    public void saveSnapshot(Path snapshotDir, boolean flush) {
        maintenanceService.createCheckpoint(snapshotDir, flush);
        LOGGER.info("RocksDB Raft snapshot saved to {}", snapshotDir);
    }

    /**
     * Read the snapshot metadata from the given directory.
     *
     * @param snapshotDir directory containing a previously created snapshot
     * @return parsed metadata as a key-value map
     * @throws StoreException if the metadata file is missing or cannot be read
     */
    public Map<String, String> getSnapshotMetadata(Path snapshotDir) {
        Path metadataFile = snapshotDir.resolve(SNAPSHOT_METADATA_FILE);
        if (!Files.exists(metadataFile)) {
            throw new StoreException("snapshot metadata file not found:" + metadataFile);
        }
        try {
            return parseMetadataFile(metadataFile);
        } catch (IOException e) {
            throw new StoreException(e, "read snapshot metadata failed:" + metadataFile);
        }
    }

    /**
     * Validate that the snapshot at the given directory is compatible with the current engine.
     *
     * <p>Checks format version and column family list against the current runtime.
     * Warnings are logged for non-fatal mismatches (e.g., different RocksDB JNI version).
     *
     * @param snapshotDir directory containing a previously created snapshot
     * @throws StoreException if the metadata is missing or format version is incompatible
     */
    public void validateSnapshotCompatibility(Path snapshotDir) {
        Map<String, String> metadata = getSnapshotMetadata(snapshotDir);

        String formatVersion = metadata.get("formatVersion");
        if (formatVersion == null) {
            throw new StoreException("snapshot metadata missing formatVersion");
        }
        int snapshotFormatVersion;
        try {
            snapshotFormatVersion = Integer.parseInt(formatVersion);
        } catch (NumberFormatException e) {
            throw new StoreException(e, "invalid snapshot formatVersion:" + formatVersion);
        }
        if (snapshotFormatVersion != RocksDBStoreEngine.FORMAT_VERSION) {
            throw new StoreException("snapshot format version mismatch, expected:" + RocksDBStoreEngine.FORMAT_VERSION
                    + ", found:" + snapshotFormatVersion);
        }

        String columnFamilies = metadata.get("columnFamilies");
        if (columnFamilies != null) {
            List<String> expectedNames = Arrays.stream(RocksDBColumnFamily.values())
                    .map(RocksDBColumnFamily::getName)
                    .collect(Collectors.toList());
            List<String> snapshotFamilies = Arrays.asList(columnFamilies.split(","));
            if (!snapshotFamilies.containsAll(expectedNames)) {
                LOGGER.warn(
                        "snapshot column families may be missing required entries, snapshot:{}, expected:{}",
                        snapshotFamilies,
                        expectedNames);
            }
        }

        RocksDB.Version version = RocksDB.rocksdbVersion();
        String currentVersion = version != null ? version.toString() : "unknown";
        String snapshotVersion = metadata.getOrDefault("rocksdbVersion", "unknown");
        if (!currentVersion.equals(snapshotVersion)) {
            LOGGER.warn(
                    "snapshot RocksDB version differs from current, snapshot:{}, current:{}",
                    snapshotVersion,
                    currentVersion);
        }

        LOGGER.info("Snapshot compatibility validated, formatVersion:{}, snapshotDir:{}", formatVersion, snapshotDir);
    }

    /**
     * Replace the local RocksDB directory contents with snapshot files.
     *
     * <p>This method is intended to be called after the engine has been closed and before
     * reopening. The caller is responsible for closing the engine before calling this method
     * and reopening it afterwards.
     *
     * <p>The target directory is cleared (all existing files are deleted) and then populated
     * with files from the snapshot directory. The metadata file is also copied so the replaced
     * directory retains provenance information.
     *
     * @param snapshotDir source snapshot directory
     * @param targetDbDir target RocksDB directory (must exist)
     * @throws StoreException if any file operation fails
     */
    public void replaceLocalDbFromSnapshot(Path snapshotDir, Path targetDbDir) {
        Path normalizedSnapshotDir = normalizePath(snapshotDir, "snapshot");
        Path normalizedTargetDbDir = normalizePath(targetDbDir, "target db");
        rejectOverlappingPaths(normalizedSnapshotDir, normalizedTargetDbDir);
        if (!Files.isDirectory(normalizedSnapshotDir)) {
            throw new StoreException("snapshot directory does not exist:" + normalizedSnapshotDir);
        }
        if (!Files.isDirectory(normalizedTargetDbDir)) {
            throw new StoreException("target db directory does not exist:" + normalizedTargetDbDir);
        }
        Path realSnapshotDir = toRealDirectoryPath(normalizedSnapshotDir, "snapshot");
        Path realTargetDbDir = toRealDirectoryPath(normalizedTargetDbDir, "target db");
        rejectOverlappingPaths(realSnapshotDir, realTargetDbDir);

        Path targetName = realTargetDbDir.getFileName();
        if (targetName == null) {
            throw new StoreException("target db directory must have a file name:" + realTargetDbDir);
        }
        Path stagingDir = realTargetDbDir.resolveSibling(targetName + ".staging-" + UUID.randomUUID());
        Path backupDir = realTargetDbDir.resolveSibling(targetName + ".backup-" + UUID.randomUUID());
        boolean targetMovedToBackup = false;

        try {
            Files.createDirectory(stagingDir);
            copyDirectoryContents(realSnapshotDir, stagingDir);
            validateRequiredColumnFamilies(stagingDir);
            try (RocksDBStoreEngine ignored = openFromSnapshot(stagingDir, true)) {
                // Opening every column family is the snapshot integrity check before replacement.
            }

            moveDirectory(realTargetDbDir, backupDir);
            targetMovedToBackup = true;
            try {
                moveDirectory(stagingDir, realTargetDbDir);
                targetMovedToBackup = false;
            } catch (IOException moveFailure) {
                try {
                    moveDirectory(backupDir, realTargetDbDir);
                    targetMovedToBackup = false;
                } catch (IOException rollbackFailure) {
                    moveFailure.addSuppressed(rollbackFailure);
                }
                throw moveFailure;
            }
        } catch (IOException e) {
            throw new StoreException(e, "replace target db directory from snapshot failed");
        } finally {
            deleteDirectoryQuietly(stagingDir);
            if (!targetMovedToBackup && Files.isDirectory(realTargetDbDir)) {
                deleteDirectoryQuietly(backupDir);
            }
        }

        LOGGER.info(
                "Local RocksDB directory replaced from snapshot, snapshotDir:{}, targetDbDir:{}",
                realSnapshotDir,
                realTargetDbDir);
    }

    /**
     * Convenience method to open a RocksDB engine from a snapshot directory.
     *
     * <p>Useful for verifying snapshot integrity by reopening the database from the checkpoint.
     *
     * @param snapshotDir directory containing a previously created snapshot
     * @param syncWrite   whether to enable sync write for the reopened engine
     * @return a new engine instance opened from the snapshot directory
     */
    public RocksDBStoreEngine openFromSnapshot(Path snapshotDir, boolean syncWrite) {
        return RocksDBStoreEngine.open(new RocksDBStoreConfig(snapshotDir.toString(), syncWrite));
    }

    /**
     * Convenience method that performs the full snapshot load flow:
     * validate compatibility, replace local DB directory from snapshot.
     *
     * <p>The caller must ensure the engine at {@code targetDbDir} has been closed before calling.
     * After this method returns, the caller should reopen the engine.
     *
     * @param snapshotDir source snapshot directory
     * @param targetDbDir target RocksDB directory to replace
     */
    public void loadSnapshot(Path snapshotDir, Path targetDbDir) {
        Path normalizedSnapshotDir = normalizePath(snapshotDir, "snapshot");
        Path normalizedTargetDbDir = normalizePath(targetDbDir, "target db");
        rejectOverlappingPaths(normalizedSnapshotDir, normalizedTargetDbDir);
        validateSnapshotCompatibility(normalizedSnapshotDir);
        replaceLocalDbFromSnapshot(normalizedSnapshotDir, normalizedTargetDbDir);
        LOGGER.info("Snapshot loaded from {} into {}", normalizedSnapshotDir, normalizedTargetDbDir);
    }

    private static Map<String, String> parseMetadataFile(Path metadataFile) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(metadataFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                result.put(trimmed.substring(0, eq), trimmed.substring(eq + 1));
            }
        }
        return result;
    }

    private static Path normalizePath(Path path, String description) {
        if (path == null) {
            throw new StoreException(description + " path must not be null");
        }
        return path.toAbsolutePath().normalize();
    }

    private static void rejectOverlappingPaths(Path snapshotDir, Path targetDbDir) {
        if (snapshotDir.equals(targetDbDir)
                || snapshotDir.startsWith(targetDbDir)
                || targetDbDir.startsWith(snapshotDir)) {
            throw new StoreException("snapshot and target db directories must not overlap, snapshot:" + snapshotDir
                    + ", target:" + targetDbDir);
        }
    }

    private static Path toRealDirectoryPath(Path path, String description) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new StoreException(e, "resolve " + description + " directory failed:" + path);
        }
    }

    private static void validateRequiredColumnFamilies(Path snapshotDir) {
        try (Options options = new Options()) {
            Set<String> existingFamilies = new HashSet<>();
            for (byte[] family : RocksDB.listColumnFamilies(options, snapshotDir.toString())) {
                existingFamilies.add(new String(family, StandardCharsets.UTF_8));
            }
            List<String> missingFamilies = Arrays.stream(RocksDBColumnFamily.values())
                    .map(RocksDBColumnFamily::getName)
                    .filter(name -> !existingFamilies.contains(name))
                    .collect(Collectors.toList());
            if (!missingFamilies.isEmpty()) {
                throw new StoreException("snapshot is missing required column families:" + missingFamilies);
            }
        } catch (RocksDBException e) {
            throw new StoreException(e, "list snapshot column families failed:" + snapshotDir);
        }
    }

    private static void copyDirectoryContents(Path sourceDir, Path targetDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path sourceFile : stream) {
                String fileName = sourceFile.getFileName().toString();
                if (SKIP_FILES.contains(fileName)) {
                    continue;
                }
                Path targetFile = targetDir.resolve(sourceFile.getFileName());
                if (Files.isDirectory(sourceFile)) {
                    copyDirectoryRecursively(sourceFile, targetFile);
                } else if (Files.isRegularFile(sourceFile)) {
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private static void deleteDirectoryQuietly(Path dir) {
        try {
            if (!Files.isDirectory(dir)) {
                return;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(dir))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                LOGGER.warn(
                                        "delete temporary snapshot file failed: {}, message: {}", p, e.getMessage());
                            }
                        });
            }
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            LOGGER.warn("delete temporary snapshot directory failed: {}, message: {}", dir, e.getMessage());
        }
    }

    private static void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
