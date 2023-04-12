/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.cluster.raft.snapshot;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.LockStatus;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.raft.session.RaftSessionManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author funkye
 */
public class SessionSnapshotFile implements StoreSnapshotFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionSnapshotFile.class);

    private static final String BRANCH_SESSION_MAP_KEY = "branchSessionMap";

    private static final String GLOBAL_SESSION_MAP_KEY = "globalSessionMap";

    String group;

    String fileName = "session";

    public SessionSnapshotFile(String group) {
        this.group = group;
    }

    /**
     * Save value to snapshot file.
     */
    public boolean save(final RaftSnapshot value, String path) {
        try {
            FileUtils.writeByteArrayToFile(new File(path), RaftSnapshotSerializer.encode(value));
            return true;
        } catch (IOException e) {
            LOGGER.error("Fail to save snapshot", e);
            return false;
        }
    }

    public Map<String, Object> load(String path) throws IOException {
        RaftSnapshot raftSnapshot = RaftSnapshotSerializer.decode(FileUtils.readFileToByteArray(new File(path)));
        final Map<String, Object> map = (Map<String, Object>)raftSnapshot.getBody();
        if (!map.isEmpty()) {
            return map;
        }
        throw new IOException("Fail to load snapshot from " + path);
    }

    @Override
    public Status save(SnapshotWriter writer) {
        // gets a record of the session at the moment
        Map<String, Object> maps = new HashMap<>(2);
        RaftSessionManager raftSessionManager = (RaftSessionManager)SessionHolder.getRootSessionManager(group);
        Map<String, GlobalSession> sessionMap = raftSessionManager.getSessionMap();
        int initialCapacity = sessionMap.size();
        Map<String, byte[]> globalSessionByteMap = new HashMap<>(initialCapacity);
        // each transaction is expected to have two branches
        Map<Long, byte[]> branchSessionByteMap = new HashMap<>(initialCapacity * 2);
        sessionMap.forEach((k, v) -> {
            globalSessionByteMap.put(v.getXid(), v.encode());
            List<BranchSession> branchSessions = Collections.unmodifiableList(v.getBranchSessions());
            branchSessions.forEach(
                branchSession -> branchSessionByteMap.put(branchSession.getBranchId(), branchSession.encode()));
        });
        maps.put(GLOBAL_SESSION_MAP_KEY, globalSessionByteMap);
        maps.put(BRANCH_SESSION_MAP_KEY, branchSessionByteMap);
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        raftSnapshot.setBody(maps);
        LOGGER.info("groupId: {}, globalSessionMap size: {}, branchSessionMap map size: {}", group,
            globalSessionByteMap.size(), branchSessionByteMap.size());
        String path = new StringBuilder(writer.getPath()).append(File.separator).append(fileName).toString();
        if (save(raftSnapshot, path)) {
            if (writer.addFile(fileName)) {
                return Status.OK();
            } else {
                return new Status(RaftError.EIO, "Fail to add file to writer");
            }
        }
        return new Status(RaftError.EIO, "Fail to save groupId: " + group + " snapshot %s", path);
    }

    @Override
    public boolean load(SnapshotReader reader) {
        String path = new StringBuilder(reader.getPath()).append(File.separator).append(fileName).toString();
        try {
            LOGGER.info("on snapshot load start index: {}", reader.load().getLastIncludedIndex());
            Map<String, Object> maps = load(path);
            RaftSessionManager raftSessionManager = (RaftSessionManager)SessionHolder.getRootSessionManager(group);
            Map<String, byte[]> globalSessionByteMap = (Map<String, byte[]>)maps.get(GLOBAL_SESSION_MAP_KEY);
            Map<Long, byte[]> branchSessionByteMap = (Map<Long, byte[]>)maps.get(BRANCH_SESSION_MAP_KEY);
            Map<String, GlobalSession> rootSessionMap = raftSessionManager.getSessionMap();
            // be sure to clear the data before loading it, because this is a full overwrite update
            LockerManagerFactory.getLockManager().cleanAllLocks();
            rootSessionMap.clear();
            if (!globalSessionByteMap.isEmpty()) {
                Map<String, GlobalSession> sessionMap = new HashMap<>();
                globalSessionByteMap.forEach((k, v) -> {
                    GlobalSession session = new GlobalSession();
                    session.decode(v);
                    sessionMap.put(k, session);
                });
                if (CollectionUtils.isNotEmpty(branchSessionByteMap)) {
                    branchSessionByteMap.forEach((k, v) -> {
                        BranchSession branchSession = new BranchSession();
                        branchSession.decode(v);
                        Optional.ofNullable(sessionMap.get(branchSession.getXid())).ifPresent(globalSession -> {
                            if (globalSession.isActive()) {
                                try {
                                    branchSession.lock();
                                } catch (TransactionException e) {
                                    LOGGER.error(e.getMessage());
                                }
                            }
                            globalSession.add(branchSession);
                        });
                    });
                    sessionMap.values().parallelStream().forEach(globalSession -> {
                        if (GlobalStatus.Rollbacking.equals(globalSession.getStatus())
                            || GlobalStatus.TimeoutRollbacking.equals(globalSession.getStatus())) {
                            globalSession.getBranchSessions().parallelStream()
                                .forEach(branchSession -> branchSession.setLockStatus(LockStatus.Rollbacking));
                        }
                    });
                }
                rootSessionMap.putAll(sessionMap);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("on snapshot load end index: {}", reader.load().getLastIncludedIndex());
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("fail to load snapshot from {}", path);
            return false;
        }
    }

}
