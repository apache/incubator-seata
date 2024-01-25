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

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;

/**
 */
public class RaftGlobalSessionSyncMsg extends RaftBaseMsg {

    private static final long serialVersionUID = -8577994371969898054L;
    private GlobalTransactionDTO globalSession;

    public RaftGlobalSessionSyncMsg(RaftSyncMsgType msgType, GlobalTransactionDTO globalSession) {
        this.msgType = msgType;
        this.globalSession = globalSession;
    }

    public RaftGlobalSessionSyncMsg() {
    }

    public GlobalTransactionDTO getGlobalSession() {
        return globalSession;
    }

    public void setGlobalSession(GlobalTransactionDTO globalSession) {
        this.globalSession = globalSession;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }


}
