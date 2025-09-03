package org.apache.seata.core.rpc.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.io.IOException;
import java.io.OutputStream;

public class ChannelOutputStreamAdapter extends OutputStream {
    private final Channel channel;
    private final byte[] singleByte = new byte[1];

    public ChannelOutputStreamAdapter(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void write(int b) throws IOException {
        singleByte[0] = (byte) b;
        channel.writeAndFlush(Unpooled.wrappedBuffer(singleByte));
    }

    @Override
    public void write(byte[] b) throws IOException {
        channel.writeAndFlush(Unpooled.wrappedBuffer(b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        channel.writeAndFlush(Unpooled.wrappedBuffer(b, off, len));
    }

    @Override
    public void flush() throws IOException {
        channel.flush();
    }
}
