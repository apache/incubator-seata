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
package io.seata.core.rpc.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.seata.common.util.StringUtils;
import io.seata.core.rpc.grpc.AbstractGrpcRemotingClient;
import io.seata.core.rpc.grpc.MetaHeaderConstants;

/**
 * @author goodboycoder
 */
public class ClientHeaderInterceptor implements ClientInterceptor {
    private final AbstractGrpcRemotingClient remotingClient;

    public ClientHeaderInterceptor(AbstractGrpcRemotingClient remotingClient) {
        this.remotingClient = remotingClient;
    }

    @Override
    public <T, S> ClientCall<T, S> interceptCall(MethodDescriptor<T, S> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<T, S>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<S> responseListener, Metadata headers) {
                String clientId = remotingClient.getClientId();
                if (StringUtils.isNotBlank(clientId)) {
                    headers.put(MetaHeaderConstants.CLIENT_ID, clientId);
                }
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<S>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}
