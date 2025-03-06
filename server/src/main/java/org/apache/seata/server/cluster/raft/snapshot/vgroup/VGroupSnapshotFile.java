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
package org.apache.seata.server.cluster.raft.snapshot.vgroup;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.cluster.raft.snapshot.RaftSnapshot;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.storage.raft.sore.RaftVGroupMappingStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VGroupSnapshotFile implements Serializable, StoreSnapshotFile {

    private final static Logger LOGGER = LoggerFactory.getLogger(VGroupSnapshotFile.class);

    public static final String ROOT_MAPPING_MANAGER_NAME = "vgroup_mapping.json";

    String group;

    public VGroupSnapshotFile(String group) {
        this.group = group;
    }

    @Override
    public Status save(SnapshotWriter writer) {
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        RaftVGroupMappingStoreManager raftVGroupMappingStoreManager =
            (RaftVGroupMappingStoreManager)SessionHolder.getRootVGroupMappingManager();
        Map<String/*vgroup*/, MappingDO> map = raftVGroupMappingStoreManager.loadVGroupsByUnit(group);
        raftSnapshot.setBody(map);
        raftSnapshot.setType(RaftSnapshot.SnapshotType.vgroup_mapping);
        String path = new StringBuilder(writer.getPath()).append(File.separator).append(ROOT_MAPPING_MANAGER_NAME).toString();
        try {
            if (save(raftSnapshot, path)) {
                if (writer.addFile(ROOT_MAPPING_MANAGER_NAME)) {
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
        if (reader.getFileMeta(ROOT_MAPPING_MANAGER_NAME) == null) {
            LOGGER.error("Fail to find data file in {}", reader.getPath());
            return false;
        }
        String path = new StringBuilder(reader.getPath()).append(File.separator).append(ROOT_MAPPING_MANAGER_NAME).toString();
        try {
            Map<String/*vgroup*/, MappingDO> map = (Map<String/*vgroup*/, MappingDO>)load(path);
            RaftVGroupMappingStoreManager raftVGroupMappingStoreManager =
                (RaftVGroupMappingStoreManager)SessionHolder.getRootVGroupMappingManager();
            raftVGroupMappingStoreManager.localAddVGroups(map, group);
            return true;
        } catch (final Exception e) {
            LOGGER.error("fail to load snapshot from {}", path, e);
            return false;
        }
    }

}
