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
import org.apache.seata.core.context.RootContext;
import org.apache.seata.integration.grpc.interceptor.GrpcHeaderKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ServerTransactionInterceptor intercepts incoming gRPC calls on the server side
 * to extract global transaction context information (XID and branch type) from request metadata,
 * and injects this context into a ServerListenerProxy to manage transaction context lifecycle.
 */
public class ServerTransactionInterceptor implements ServerInterceptor {

    /**
     * Intercepts a gRPC call to extract transaction context and wrap the ServerCall.Listener.
     *
     * @param serverCall       the gRPC ServerCall object
     * @param metadata         the request metadata (headers)
     * @param serverCallHandler the next handler in the interceptor chain
     * @param <ReqT>           the request type
     * @param <RespT>          the response type
     * @return a wrapped ServerCall.Listener that manages transaction context
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String xid = getRpcXid(metadata);
        String branchName = getBranchName(metadata);
        Map<String, String> context = new HashMap<>();
        context.put(RootContext.KEY_BRANCH_TYPE, branchName);
        return new ServerListenerProxy<>(
                xid, Collections.unmodifiableMap(context), serverCallHandler.startCall(serverCall, metadata));
    }

    /**
     * Extracts the global transaction ID (XID) from metadata headers,
     * supporting both uppercase and lowercase keys.
     */
    private String getRpcXid(Metadata metadata) {
        String rpcXid = metadata.get(GrpcHeaderKey.XID_HEADER_KEY);
        if (rpcXid == null) {
            rpcXid = metadata.get(GrpcHeaderKey.XID_HEADER_KEY_LOWERCASE);
        }
        return rpcXid;
    }

    /**
     * Extracts the branch transaction type name from metadata headers.
     */
    private String getBranchName(Metadata metadata) {
        return metadata.get(GrpcHeaderKey.BRANCH_HEADER_KEY);
    }
}
