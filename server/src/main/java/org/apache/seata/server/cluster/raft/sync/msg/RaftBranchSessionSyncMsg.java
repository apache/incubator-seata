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

import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;

/**
 */
public class RaftBranchSessionSyncMsg extends RaftBaseMsg {

    private static final long serialVersionUID = -8577994371969898054L;

    private BranchTransactionDTO branchSession;

    private String group = DEFAULT_SEATA_GROUP;

    public RaftBranchSessionSyncMsg(RaftSyncMsgType msgType, BranchTransactionDTO branchSession) {
        this.msgType = msgType;
        this.branchSession = branchSession;
    }

    public RaftBranchSessionSyncMsg() {}

    public BranchTransactionDTO getBranchSession() {
        return branchSession;
    }

    public void setBranchSession(BranchTransactionDTO branchSession) {
        this.branchSession = branchSession;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
