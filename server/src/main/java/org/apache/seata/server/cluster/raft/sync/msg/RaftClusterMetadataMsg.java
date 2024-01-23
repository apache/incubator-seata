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
package org.apache.seata.server.cluster.raft.sync.msg;

import io.seata.common.util.StringUtils;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;

/**
 */
public class RaftClusterMetadataMsg extends RaftBaseMsg {

    private static final long serialVersionUID = 6208583637662412658L;

    private RaftClusterMetadata raftClusterMetadata;

    public RaftClusterMetadataMsg(RaftClusterMetadata raftClusterMetadata) {
        this.msgType = RaftSyncMsgType.REFRESH_CLUSTER_METADATA;
        this.raftClusterMetadata = raftClusterMetadata;
    }

    public RaftClusterMetadataMsg() {
    }

    public RaftClusterMetadata getRaftClusterMetadata() {
        return raftClusterMetadata;
    }

    public void setRaftClusterMetadata(RaftClusterMetadata raftClusterMetadata) {
        this.raftClusterMetadata = raftClusterMetadata;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
