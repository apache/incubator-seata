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
package io.seata.server.cluster.raft.sync.msg;

import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;

import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * @author funkye
 */
public class RaftSessionSyncMsg implements java.io.Serializable {

    private static final long serialVersionUID = -6737504033652157760L;

    private GlobalTransactionDO globalSession;

    private BranchTransactionDO branchSession;

    private String group = DEFAULT_SEATA_GROUP;

    private MsgType msgType;

    public RaftSessionSyncMsg(MsgType msgType, GlobalTransactionDO globalSession) {
        this.msgType = msgType;
        this.globalSession = globalSession;
    }

    public RaftSessionSyncMsg(MsgType msgType, BranchTransactionDO branchSession) {
        this.msgType = msgType;
        this.branchSession = branchSession;
    }

    public RaftSessionSyncMsg(MsgType msgType, GlobalTransactionDO globalSession, BranchTransactionDO branchSession) {
        this.msgType = msgType;
        this.globalSession = globalSession;
        this.branchSession = branchSession;
    }

    public RaftSessionSyncMsg() {}

    public GlobalTransactionDO getGlobalSession() {
        return globalSession;
    }

    public void setGlobalSession(GlobalTransactionDO globalSession) {
        this.globalSession = globalSession;
    }

    public BranchTransactionDO getBranchSession() {
        return branchSession;
    }

    public void setBranchSession(BranchTransactionDO branchSession) {
        this.branchSession = branchSession;
    }

    public MsgType getMsgType() {
        return this.msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public enum MsgType {
        /**
         * addGlobalSession
         */
        ADD_GLOBAL_SESSION,
        /**
         * removeGlobalSession
         */
        REMOVE_GLOBAL_SESSION,
        /**
         *
         */
        ADD_BRANCH_SESSION,
        /**
         * addBranchSession
         */
        REMOVE_BRANCH_SESSION,
        /**
         * updateGlobalSessionStatus
         */
        UPDATE_GLOBAL_SESSION_STATUS,
        /**
         * updateBranchSessionStatus
         */
        UPDATE_BRANCH_SESSION_STATUS,
        /**
         * releaseGlobalSessionLock
         */
        RELEASE_GLOBAL_SESSION_LOCK,
        /**
         * releaseBranchSessionLock
         */
        RELEASE_BRANCH_SESSION_LOCK,
        /**
         * ServerOnRequestProcessor
         */
        SERVER_ON_REQUEST
    }

    @Override
    public String toString() {
        return "RaftSessionSyncMsg{" + "globalSession=" + globalSession + ", branchSession=" + branchSession
            + ", group='" + group + '\'' + ", msgType=" + msgType + '}';
    }

}
