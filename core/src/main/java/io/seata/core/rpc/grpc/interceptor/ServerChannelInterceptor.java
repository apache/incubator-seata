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

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.internal.ServerStream;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.seata.common.util.ReflectionUtil;
import io.seata.common.util.StringUtils;
import io.seata.core.rpc.grpc.ContextKeyConstants;
import io.seata.core.rpc.grpc.MetaHeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class ServerChannelInterceptor implements ServerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerChannelInterceptor.class);

    @Override
    public <R, S> ServerCall.Listener<R> interceptCall(ServerCall<R, S> call, Metadata headers, ServerCallHandler<R, S> next) {
        Context ctx = Context.current();
        //get clientId from header
        String clientId = headers.get(MetaHeaderConstants.CLIENT_ID);
        if (StringUtils.isBlank(clientId)) {
            //get current connection ID from serverCall
            clientId = call.getAttributes().get(ContextKeyConstants.CONNECT_ID);
        }
        ctx = ctx.withValue(ContextKeyConstants.CUR_CONNECT_ID, clientId);

        //get internal channel from serverCall
        try {
            ServerStream stream = ReflectionUtil.getFieldValue(call, "stream");
            // grpc-netty-shaded removed the "channel" field in version 1.31.0,
            // which means that the method cannot get the channel in version 1.31.0 and above
            Channel channel = ReflectionUtil.getFieldValue(stream, "channel");
            ctx = ctx.withValue(ContextKeyConstants.CUR_CONNECTION, channel);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
