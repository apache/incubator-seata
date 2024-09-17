package org.apache.seata.namingserver.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class NamingServerHttp2Handler extends ChannelDuplexHandler {

    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    private ClusterWatcherManager clusterWatcherManager;

    public NamingServerHttp2Handler(ClusterWatcherManager clusterWatcherManager) {
        this.clusterWatcherManager = clusterWatcherManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http2HeadersFrame) {
            if (headerSent.compareAndSet(false, true)) {
                Http2Headers headers = new DefaultHttp2Headers();
                headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
            }

            Http2HeadersFrame http2HeadersFrame = (Http2HeadersFrame) msg;
            CharSequence groupChar = http2HeadersFrame.headers().get("seata-group");
            CharSequence termChar = http2HeadersFrame.headers().get("seata-term");
            String group = groupChar.toString();
            long term = Long.parseLong(termChar.toString());
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            Watcher<Channel> watcher = new Watcher<>(group, ctx.channel(), -1, term, inetSocketAddress.getAddress().getHostAddress());
            clusterWatcherManager.registryWatcher(watcher);
        }
    }
}
