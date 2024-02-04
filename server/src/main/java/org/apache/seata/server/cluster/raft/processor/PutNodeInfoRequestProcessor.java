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
package org.apache.seata.server.cluster.raft.processor;

import java.util.List;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.server.cluster.raft.RaftServer;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.RaftStateMachine;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.PutNodeMetadataResponse;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;

public class PutNodeInfoRequestProcessor implements RpcProcessor<PutNodeMetadataRequest> {

    public PutNodeInfoRequestProcessor() {
        super();
    }

    @Override
    public void handleRequest(RpcContext rpcCtx, PutNodeMetadataRequest request) {
        Node node = request.getNode();
		String group = node.getGroup();
        if (RaftServerManager.isLeader(group)) {
			RaftServer raftServer = RaftServerManager.getRaftServer(group);
			RaftStateMachine raftStateMachine = raftServer.getRaftStateMachine();
            try {
                RaftClusterMetadata raftClusterMetadata = raftStateMachine.getRaftLeaderMetadata();
                List<Node> followers = raftClusterMetadata.getFollowers();
                for (Node follower : followers) {
                    Node.Endpoint endpoint = follower.getInternal();
                    if (endpoint != null) {
						// change old follower node metadata
                        if (endpoint.getHost().equals(node.getInternal().getHost())
                            && endpoint.getPort() == node.getInternal().getPort()) {
                            follower.setTransaction(node.getTransaction());
                            follower.setControl(node.getControl());
                            follower.setGroup(group);
                            follower.setMetadata(node.getMetadata());
                            follower.setVersion(node.getVersion());
                            return;
                        }
                    }
                }
				// add new follower node metadata
                followers.add(node);
            } finally {
                rpcCtx.sendResponse(new PutNodeMetadataResponse(true));
				raftStateMachine.syncMetadata();
            }
        }
    }

    @Override
    public String interest() {
        return PutNodeMetadataRequest.class.getName();
    }

}
