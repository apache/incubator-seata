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

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.apache.seata.integration.grpc.interceptor.GrpcHeaderKey;
import org.apache.seata.integration.rpc.core.TransactionPropagationHandler;

public class ServerTransactionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String rpcXid = getRpcXid(metadata);
        String rpcBranchType = metadata.get(GrpcHeaderKey.BRANCH_HEADER_KEY);
        return new ServerListenerProxy<>(rpcXid, rpcBranchType, serverCallHandler.startCall(serverCall, metadata));
    }

    private String getRpcXid(Metadata metadata) {
        return TransactionPropagationHandler.resolveXid(
                metadata.get(GrpcHeaderKey.XID_HEADER_KEY), metadata.get(GrpcHeaderKey.XID_HEADER_KEY_LOWERCASE));
    }
}
