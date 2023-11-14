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

import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * @author jianbin.chen
 */
public class RaftBaseMsg implements java.io.Serializable {

    private static final long serialVersionUID = -1439073440621259777L;

    protected RaftSyncMsgType msgType;

    protected String group = DEFAULT_SEATA_GROUP;

    public RaftSyncMsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(RaftSyncMsgType msgType) {
        this.msgType = msgType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

}
