package io.seata.core.rpc.grpc;

import io.grpc.Attributes;
import io.grpc.Channel;
import io.grpc.Context;
import io.grpc.internal.ServerStream;

/**
 * @author goodboycoder
 */
public class ContextKeyConstants {
    public static final Attributes.Key<String> CONNECT_ID = Attributes.Key.create("connect_id");
    public static final Context.Key<Channel> CUR_CONNECTION = Context.key("cur_connection");
}
