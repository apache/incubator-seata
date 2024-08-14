package org.apache.seata.core.protocol.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.CharsetUtil;
import org.apache.seata.core.protocol.detector.ProtocolDetector;
import org.apache.seata.core.rpc.netty.grpc.GrpcDecoder;
import org.apache.seata.core.rpc.netty.grpc.GrpcEncoder;

public class Http2Detector implements ProtocolDetector {
    private final byte[] HTTP2_PREFIX_BYTES = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(CharsetUtil.UTF_8);
    private ChannelHandler[] serverHandlers;

    public Http2Detector(ChannelHandler[] serverHandlers) {
        this.serverHandlers = serverHandlers;
    }

    @Override
    public boolean detect(ByteBuf in) {
        if (in.readableBytes() < HTTP2_PREFIX_BYTES.length) {
            return false;
        }
        for (int i = 0; i < HTTP2_PREFIX_BYTES.length; i++) {
            if (in.getByte(i) != HTTP2_PREFIX_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        return new ChannelHandler[]{
                Http2FrameCodecBuilder.forServer().build(),
                new Http2MultiplexHandler(new ChannelInitializer<Http2StreamChannel>() {
                    @Override
                    protected void initChannel(Http2StreamChannel ch) {
                        final ChannelPipeline p = ch.pipeline();
                        p.addLast(new GrpcDecoder());
                        p.addLast(new GrpcEncoder());
                        p.addLast(serverHandlers);
                    }
                })
        };
    }
}