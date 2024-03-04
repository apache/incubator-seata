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
package org.apache.seata.server.cluster.raft.snapshot.metadata;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.snapshot.RaftSnapshot;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LeaderMetadataSnapshotFile implements Serializable, StoreSnapshotFile {
    private static final long serialVersionUID = 78637164618855724L;

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderMetadataSnapshotFile.class);

    private final String group;

    private final String fileName = "leader_metadata";

    public LeaderMetadataSnapshotFile(String group) {
        this.group = group;
    }

    @Override
    public Status save(SnapshotWriter writer) {
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        RaftClusterMetadata raftClusterMetadata =
            RaftServerManager.getRaftServer(group).getRaftStateMachine().getRaftLeaderMetadata();
        raftSnapshot.setBody(raftClusterMetadata);
        raftSnapshot.setType(RaftSnapshot.SnapshotType.leader_metadata);
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
            RaftClusterMetadata raftClusterMetadata = (RaftClusterMetadata)load(path);
            RaftServerManager.getRaftServer(group).getRaftStateMachine()
                .setRaftLeaderMetadata(raftClusterMetadata);
            return true;
        } catch (final Exception e) {
            LOGGER.error("fail to load snapshot from {}", path, e);
            return false;
        }
    }
}
