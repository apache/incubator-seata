package org.apache.seata.namingserver.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import org.apache.seata.core.protocol.detector.Http2Detector;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;

public class NamingServerHttp2Detector extends Http2Detector {
    ClusterWatcherManager clusterWatcherManager;
    public NamingServerHttp2Detector(ClusterWatcherManager clusterWatcherManager) {
        super(null);
        this.clusterWatcherManager = clusterWatcherManager;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        return new ChannelHandler[]{
                Http2FrameCodecBuilder.forServer().build(),
                new Http2MultiplexHandler(new ChannelInitializer<Http2StreamChannel>() {
                    @Override
                    protected void initChannel(Http2StreamChannel ch) {
                        final ChannelPipeline p = ch.pipeline();
                        p.addLast(new NamingServerHttp2Handler(clusterWatcherManager));
                    }
                })
        };
    }
}
