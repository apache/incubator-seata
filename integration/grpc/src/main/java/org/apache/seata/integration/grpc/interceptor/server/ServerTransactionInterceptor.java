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
package org.apache.seata.integration.grpc.interceptor.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.integration.grpc.interceptor.GrpcHeaderKey;


public class ServerTransactionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall,
        Metadata metadata,
        ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String xid = getRpcXid(metadata);
        String branchName = getBranchName(metadata);
        Map<String, String> context = new HashMap<>();
        context.put(RootContext.KEY_BRANCH_TYPE, branchName);
        return new ServerListenerProxy<>(xid, Collections.unmodifiableMap(context),
            serverCallHandler.startCall(serverCall, metadata));
    }

    /**
     * get rpc xid
     * @param metadata
     * @return
     */
    private String getRpcXid(Metadata metadata) {
        String rpcXid = metadata.get(GrpcHeaderKey.XID_HEADER_KEY);
        if (rpcXid == null) {
            rpcXid = metadata.get(GrpcHeaderKey.XID_HEADER_KEY_LOWERCASE);
        }
        return rpcXid;
    }

    private String getBranchName(Metadata metadata) {
        return metadata.get(GrpcHeaderKey.BRANCH_HEADER_KEY);
    }

}
