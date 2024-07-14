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

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.server.cluster.raft.*;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.PutNodeMetadataResponse;

import static org.apache.seata.common.Constants.RAFT_CONFIG_GROUP;

public class PutNodeInfoRequestProcessor implements RpcProcessor<PutNodeMetadataRequest> {

    public PutNodeInfoRequestProcessor() {
        super();
    }

    @Override
    public void handleRequest(RpcContext rpcCtx, PutNodeMetadataRequest request) {
        Node node = request.getNode();
        String group = node.getGroup();
        if (RaftConfigServerManager.getGroup().equals(group)){
            changeConfigGroupRequest(group, node, rpcCtx, request);
        }else{
            changeNormalGroupRequest(group, node, rpcCtx, request);
        }
    }

    private static void changeConfigGroupRequest(String group, Node node, RpcContext rpcCtx, PutNodeMetadataRequest request){
        if (RaftConfigServerManager.isLeader()) {
            RaftConfigServer raftServer = RaftConfigServerManager.getRaftServer();
            RaftConfigStateMachine raftStateMachine = raftServer.getRaftStateMachine();
            raftStateMachine.changeNodeMetadata(node);
            rpcCtx.sendResponse(new PutNodeMetadataResponse(true));
        } else {
            rpcCtx.sendResponse(new PutNodeMetadataResponse(false));
        }
    }

    private static void changeNormalGroupRequest(String group, Node node, RpcContext rpcCtx, PutNodeMetadataRequest request){
        if (RaftServerManager.isLeader(group)) {
            RaftServer raftServer = RaftServerManager.getRaftServer(group);
            RaftStateMachine raftStateMachine = raftServer.getRaftStateMachine();
            raftStateMachine.changeNodeMetadata(node);
            rpcCtx.sendResponse(new PutNodeMetadataResponse(true));
        } else {
            rpcCtx.sendResponse(new PutNodeMetadataResponse(false));
        }
    }

    @Override
    public String interest() {
        return PutNodeMetadataRequest.class.getName();
    }

}
