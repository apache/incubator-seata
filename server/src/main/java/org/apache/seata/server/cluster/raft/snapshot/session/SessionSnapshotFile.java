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
package org.apache.seata.server.cluster.raft.snapshot.session;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.server.cluster.raft.snapshot.RaftSnapshot;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.storage.raft.session.RaftSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SessionSnapshotFile implements Serializable,StoreSnapshotFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionSnapshotFile.class);

    private static final long serialVersionUID = 7942307427240595916L;

    String group;

    String fileName = "session";

    public SessionSnapshotFile(String group) {
        this.group = group;
    }

    @Override
    public Status save(SnapshotWriter writer) {
        RaftSessionManager raftSessionManager = (RaftSessionManager)SessionHolder.getRootSessionManager(group);
        Map<String, GlobalSession> sessionMap = raftSessionManager.getSessionMap();
        RaftSessionSnapshot sessionSnapshot = new RaftSessionSnapshot();
        sessionMap.forEach((xid, session) -> sessionSnapshot.convert2GlobalSessionByte(session));
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        raftSnapshot.setBody(sessionSnapshot);
        raftSnapshot.setType(RaftSnapshot.SnapshotType.session);
        LOGGER.info("groupId: {}, global session size: {}", group, sessionSnapshot.getGlobalsessions().size());
        String path = new StringBuilder(writer.getPath()).append(File.separator).append(fileName).toString();
        try {
            if (save(raftSnapshot, path)) {
                if (writer.addFile(fileName)) {
                    return Status.OK();
                } else {
                    return new Status(RaftError.EIO, "Fail to add file to writer");
                }
            }
        } catch (IOException e) {
            LOGGER.error("Fail to save groupId: {} snapshot {}", group, path, e);
        }
        return new Status(RaftError.EIO, "Fail to save groupId: " + group + " snapshot %s", path);
    }

    @Override
    public boolean load(SnapshotReader reader) {
        if (reader.getFileMeta(fileName) == null) {
            LOGGER.error("Fail to find data file in {}", reader.getPath());
            return false;
        }
        String path = new StringBuilder(reader.getPath()).append(File.separator).append(fileName).toString();
        try {
            LOGGER.info("on snapshot load start index: {}", reader.load().getLastIncludedIndex());
            RaftSessionSnapshot sessionSnapshot = (RaftSessionSnapshot)load(path);
            RaftSessionManager raftSessionManager = (RaftSessionManager)SessionHolder.getRootSessionManager(group);
            Map<String, GlobalSession> rootSessionMap = raftSessionManager.getSessionMap();
            // be sure to clear the data before loading it, because this is a full overwrite update
            LockerManagerFactory.getLockManager().cleanAllLocks();
            rootSessionMap.clear();
            rootSessionMap.putAll(sessionSnapshot.convert2GlobalSession());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("on snapshot load end index: {}", reader.load().getLastIncludedIndex());
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("fail to load snapshot from {}", path, e);
            return false;
        }
    }

}
