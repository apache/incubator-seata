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
package io.seata.serializer.seata.protocol.gts;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.rpc.netty.gts.message.BeginMessage;
import io.seata.core.rpc.netty.gts.message.GtsRpcMessage;
import io.seata.core.rpc.netty.gts.message.TxcCodec;
import io.seata.core.rpc.netty.v1.ProtocolV1Decoder;
import io.seata.core.rpc.netty.v1.ProtocolV1Encoder;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

/**
 * @author cj3
 */
public class GtsSeataMessageTest {
    private static final short MAGIC = -9510;

    @Test
    public void testGtsSeataMessageTest() throws InterruptedException, IllegalAccessException, TimeoutException {
        try {
            new EchoClient("127.0.0.1", 8091).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class EchoClient {

    private final String host;
    private final int port;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 程序绑定流程
     */
    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).remoteAddress(new InetSocketAddress(host, port)).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new EchoClientHandler());
                }
            });
            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

}

@ChannelHandler.Sharable
class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private ChannelHandlerContext ctx;
    private ByteBuf in;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws UnsupportedEncodingException, InterruptedException {
        ByteBuf buff = Unpooled.buffer();
        ctx.writeAndFlush(buff);
        try {
            ctx.writeAndFlush(getByteBuf(false));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(1000);
        try {
            ctx.writeAndFlush(getByteBuf(true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        this.ctx = ctx;
        this.in = in;
        System.out.println("Client received: " + in.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


    public ByteBuf getByteBuf(boolean chooseGts) throws InterruptedException, IllegalAccessException, TimeoutException {
        short MAGIC = -9510;
        BeginMessage msg = new BeginMessage();
        msg.setAppname("default");
        msg.setTxcInst("com.taobao.txc.sample.BizService.doTransfer");
        System.out.println(msg);
        byte[] bs = msg.encode();
        GtsRpcMessage rpcMessage = new GtsRpcMessage();
        rpcMessage.setId(1L);
        rpcMessage.setBody(msg);

        TxcCodec txcCodec = null;
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        if (rpcMessage.getBody() instanceof TxcCodec) {
            txcCodec = (TxcCodec) rpcMessage.getBody();
        }
        byteBuffer.putShort(MAGIC);
        int flag = (rpcMessage.isAsync() ? 64 : 0) | (rpcMessage.isHeartbeat() ? 32 : 0) | (rpcMessage.isRequest() ? 128 : 0) | (txcCodec != null ? 16 : 0);
        byteBuffer.putShort((short) flag);
        byte[] content;
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer(128);
        byteBuffer.putShort(txcCodec.getTypeCode());
        byteBuffer.putLong(rpcMessage.getId());
        byteBuffer.flip();
        content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        out.writeBytes(content);
        out.writeBytes(txcCodec.encode());
        ProtocolV1Decoder decoder = new ProtocolV1Decoder();
        ProtocolV1Encoder encoder = new ProtocolV1Encoder();

        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("xx");
        globalBeginRequest.setTimeout(3000);

        ByteBuf seataOut = ByteBufAllocator.DEFAULT.buffer(128);
        RpcMessage seatarpcMessage = new RpcMessage();
        seatarpcMessage.setBody(globalBeginRequest);
        seatarpcMessage.setCodec((byte) 1);
        encoder.encode(null, seatarpcMessage, seataOut);

        if (chooseGts) return out;
        else return seataOut;
    }

}

