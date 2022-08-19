package io.seata.core.rpc.grpc;

import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.netty.shaded.io.netty.channel.Channel;

/**
 * @author goodboycoder
 */
public class ContextKeyConstants {
    public static final Attributes.Key<String> CONNECT_ID = Attributes.Key.create("connect_id");

    public static final Context.Key<String> CUR_CONNECT_ID = Context.key("connect_id");
    public static final Context.Key<Channel> CUR_CONNECTION = Context.key("cur_connection");
}
