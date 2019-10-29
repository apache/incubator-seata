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
package io.seata.integration.grpc.interceptor.server;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.seata.integration.grpc.interceptor.GrpcHeaderKey;

/**
 * @author eddyxu1213@126.com
 */
public class ServerTransactionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall,
        Metadata metadata,
        ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String xid = getRpcXid(metadata);
        return new ServerListenerProxy<>(xid, serverCallHandler.startCall(serverCall, metadata));
    }

    /**
     * get rpc xid
     * @param metadata
     * @return
     */
    private String getRpcXid(Metadata metadata) {
        String rpcXid = metadata.get(GrpcHeaderKey.HEADER_KEY);
        if (rpcXid == null) {
            rpcXid = metadata.get(GrpcHeaderKey.HEADER_KEY_LOWERCASE);
        }
        return rpcXid;
    }

}
