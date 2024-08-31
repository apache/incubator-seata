package org.apache.seata.core.protocol.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.apache.seata.core.rpc.netty.http.HttpDispatchHandler;

public class HttpDetector implements ProtocolDetector {
    private static final String[] HTTP_METHODS = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"};

    @Override
    public boolean detect(ByteBuf in) {
        if (in.readableBytes() < 8) {
            return false;
        }

        for (String method : HTTP_METHODS) {
            if (startsWith(in, method)) {
                return true;
            }
        }

        return false;
    }

    private boolean startsWith(ByteBuf buffer, String prefix) {
        for (int i = 0; i < prefix.length(); i++) {
            if (buffer.getByte(i) != (byte) prefix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        return new ChannelHandler[]{
            new HttpServerCodec(),
            new HttpObjectAggregator(1048576),
            new HttpDispatchHandler()
        };
    }
}