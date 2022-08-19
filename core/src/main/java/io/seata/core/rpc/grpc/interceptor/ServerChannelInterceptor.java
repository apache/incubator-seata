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
import io.seata.core.rpc.grpc.ContextKeyConstants;

/**
 * @author goodboycoder
 */
public class ServerChannelInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current();
        //get current connection ID from serverCall
        String connectionId = call.getAttributes().get(ContextKeyConstants.CONNECT_ID);
        ctx.withValue(ContextKeyConstants.CUR_CONNECT_ID, connectionId);

        //get internal channel from serverCall
        try {
            ServerStream stream = ReflectionUtil.getFieldValue(call, "stream");
            Channel channel = ReflectionUtil.getFieldValue(stream, "channel");
            ctx.withValue(ContextKeyConstants.CUR_CONNECTION, channel);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
