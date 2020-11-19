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
package io.seata.server.storage.raft;

import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.raft.msg.RaftSyncMsg;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;

/**
 * @author funkye
 */
public class RaftSessionSyncMsg extends RaftSyncMsg implements java.io.Serializable {

    GlobalTransactionDO globalSession;

    BranchTransactionDO branchSession;

    GlobalStatus globalStatus;

    BranchStatus branchStatus;

    String sessionName;

    public RaftSessionSyncMsg(MsgType msgType, GlobalTransactionDO globalSession) {
        this.msgType = msgType;
        this.globalSession = globalSession;
    }

    public RaftSessionSyncMsg(MsgType msgType, GlobalTransactionDO globalSession, GlobalStatus globalStatus) {
        this.msgType = msgType;
        this.globalSession = globalSession;
        this.globalStatus = globalStatus;
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

    public RaftSessionSyncMsg(MsgType msgType, BranchTransactionDO branchSession, BranchStatus branchStatus) {
        this.msgType = msgType;
        this.branchSession = branchSession;
        this.branchStatus = branchStatus;
    }

    @Override
    public MsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }

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

    public GlobalStatus getGlobalStatus() {
        return globalStatus;
    }

    public void setGlobalStatus(GlobalStatus globalStatus) {
        this.globalStatus = globalStatus;
    }

    public BranchStatus getBranchStatus() {
        return branchStatus;
    }

    public void setBranchStatus(BranchStatus branchStatus) {
        this.branchStatus = branchStatus;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    @Override public String toString() {
        return "RaftSyncMsg{" + "msgType=" + msgType + ", globalSession=" + globalSession + ", branchSession="
            + branchSession + ", globalStatus=" + globalStatus + ", branchStatus=" + branchStatus + ", sessionName='"
            + sessionName + '\'' + '}';
    }
}
