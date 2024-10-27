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
package org.apache.seata.server.cluster.raft.sync.msg.closure;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import org.apache.seata.server.cluster.raft.processor.response.ConfigOperationResponse;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;

/**
 * The type of closure for configuration sync in raft
 */
public class ConfigClosure  implements Closure {

    private RaftBaseMsg raftBaseMsg;
    private ConfigOperationResponse response;
    private Closure done;

    @Override
    public void run(Status status) {
        if (done != null) {
            done.run(status);
        }
    }

    public RaftBaseMsg getRaftBaseMsg() {
        return raftBaseMsg;
    }

    public void setRaftBaseMsg(RaftBaseMsg raftBaseMsg) {
        this.raftBaseMsg = raftBaseMsg;
    }

    public ConfigOperationResponse getResponse() {
        return response;
    }

    public void setResponse(ConfigOperationResponse response) {
        this.response = response;
    }

    public Closure getDone() {
        return done;
    }

    public void setDone(Closure done) {
        this.done = done;
    }
}
