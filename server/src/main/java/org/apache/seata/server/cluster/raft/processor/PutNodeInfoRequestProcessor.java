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
import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.server.cluster.raft.RaftServer;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.RaftStateMachine;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeInfoRequest;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.zookeeper.server.quorum.Follower;

public class PutNodeInfoRequestProcessor extends AsyncUserProcessor<PutNodeInfoRequest> {

	public PutNodeInfoRequestProcessor() {
		super();
	}

	@Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, PutNodeInfoRequest request) {
		Node node = request.getNode();
		if(RaftServerManager.isLeader(node.getGroup())){
			RaftServer raftServer = RaftServerManager.getRaftServer(node.getGroup());
			RaftStateMachine raftStateMachine = raftServer.getRaftStateMachine();
			RaftClusterMetadata raftClusterMetadata = raftStateMachine.getRaftLeaderMetadata();
			List<Node> followers = raftClusterMetadata.getFollowers();
			followers.forEach(follower -> {
				if(follower.getInternal()!=null){
					if(follower.getInternal().getHost().equals(node.getInternal().getHost())){
						follower.setTransaction(node.getTransaction());
						follower.setControl(node.getControl());
						follower.setGroup(node.getGroup());
						follower.setMetadata(node.getMetadata());
					}
				}
			});
		}
	}

    @Override
    public String interest() {
        return null;
    }

}
