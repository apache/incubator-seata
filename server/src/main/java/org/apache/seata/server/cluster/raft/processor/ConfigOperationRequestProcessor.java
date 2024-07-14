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
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.cluster.raft.RaftConfigServerManager;
import org.apache.seata.server.cluster.raft.processor.request.ConfigOperationRequest;
import org.apache.seata.server.cluster.raft.processor.response.ConfigOperationResponse;
import org.apache.seata.server.cluster.raft.sync.msg.RaftConfigOperationSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.closure.ConfigClosure;
import org.apache.seata.server.cluster.raft.sync.msg.dto.ConfigOperationDTO;
import org.apache.seata.server.cluster.raft.util.RaftConfigTaskUtil;


public class ConfigOperationRequestProcessor implements RpcProcessor<ConfigOperationRequest> {
    private static final String NOT_LEADER = "not leader";
    @Override
    public void handleRequest(RpcContext rpcCtx, ConfigOperationRequest request) {
        if (RaftConfigServerManager.isLeader()){
            onExecute(rpcCtx, request);
        }else{
            rpcCtx.sendResponse(ConfigOperationResponse.fail(NOT_LEADER));
        }
    }

    private void onExecute(RpcContext rpcCtx, ConfigOperationRequest request) {
        ConfigOperationDTO operationDTO = ConfigOperationDTO.convertConfigRequest2Dto(request);
        RaftConfigOperationSyncMsg syncMsg = new RaftConfigOperationSyncMsg(operationDTO);
        ConfigOperationResponse response = new ConfigOperationResponse();
        ConfigClosure closure = new ConfigClosure();
        closure.setRaftBaseMsg(syncMsg);
        closure.setResponse(response);
        closure.setDone(status -> {
            if (!status.isOk()){
                response.setSuccess(false);
                response.setErrMsg(status.getErrorMsg());
            }
            rpcCtx.sendResponse(response);
        });
        try {
            RaftConfigTaskUtil.createTask(closure, syncMsg, null);
        } catch (TransactionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String interest() {
        return ConfigOperationRequest.class.getName();
    }
}
