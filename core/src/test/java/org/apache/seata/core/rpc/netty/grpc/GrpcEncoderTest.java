/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.rpc.netty.grpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.detector.Http2Detector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GrpcEncoderTest {
    private GrpcEncoder grpcEncoder;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private ChannelPromise promise;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        grpcEncoder = new GrpcEncoder();
    }

    private RpcMessage createHeartbeatMessage(byte messageType, Object body) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setMessageType(messageType);
        rpcMessage.setBody(body);
        rpcMessage.setId(1);
        rpcMessage.setHeadMap(new HashMap<>());
        return rpcMessage;
    }

    private Http2Headers encodeAndCaptureHeaders(GrpcEncoder encoder, RpcMessage rpcMessage) throws Exception {
        encoder.write(ctx, rpcMessage, promise);

        ArgumentCaptor<DefaultHttp2HeadersFrame> headersCaptor =
                ArgumentCaptor.forClass(DefaultHttp2HeadersFrame.class);
        verify(ctx, times(1)).writeAndFlush(headersCaptor.capture());
        verify(ctx, times(1)).writeAndFlush(any(DefaultHttp2DataFrame.class), eq(promise));
        return headersCaptor.getValue().headers();
    }

    @Test
    public void testWrite() throws Exception {
        RpcMessage rpcMessage =
                createHeartbeatMessage(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, HeartbeatMessage.PING);

        grpcEncoder.write(ctx, rpcMessage, promise);

        verify(ctx, times(1)).writeAndFlush(any(DefaultHttp2HeadersFrame.class));
        verify(ctx, times(1)).writeAndFlush(any(DefaultHttp2DataFrame.class), eq(promise));
    }

    @Test
    public void testClientModeHeaders() throws Exception {
        Http2Headers headers = encodeAndCaptureHeaders(
                new GrpcEncoder(false),
                createHeartbeatMessage(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, HeartbeatMessage.PING));

        assertEquals(HttpMethod.POST.asciiName().toString(), headers.method().toString());
        assertEquals(HttpScheme.HTTP.name().toString(), headers.scheme().toString());
        assertEquals("/", headers.path().toString());
        assertEquals(
                "application/grpc",
                headers.get(GrpcHeaderEnum.GRPC_CONTENT_TYPE.header).toString());
        assertNull(headers.status());
        assertNull(headers.get(GrpcHeaderEnum.GRPC_STATUS.header));
    }

    @Test
    public void testServerModeHeaders() throws Exception {
        Http2Headers headers = encodeAndCaptureHeaders(
                new GrpcEncoder(true),
                createHeartbeatMessage(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE, HeartbeatMessage.PONG));

        assertEquals("200", headers.status().toString());
        assertEquals("0", headers.get(GrpcHeaderEnum.GRPC_STATUS.header).toString());
        assertEquals(
                "application/grpc",
                headers.get(GrpcHeaderEnum.GRPC_CONTENT_TYPE.header).toString());
        assertNull(headers.method());
        assertNull(headers.scheme());
        assertNull(headers.path());
    }

    @Test
    public void testHttp2ClientServerRoundTripWithoutRstStream() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(2);
        Channel serverChannel = null;
        Channel clientChannel = null;
        Http2StreamChannel streamChannel = null;
        try {
            CompletableFuture<RpcMessage> responseFuture = new CompletableFuture<>();
            AtomicReference<Http2ResetFrame> resetFrameRef = new AtomicReference<>();

            ChannelHandler serverHandler = new SimpleChannelInboundHandler<RpcMessage>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
                    RpcMessage response =
                            createHeartbeatMessage(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE, HeartbeatMessage.PONG);
                    response.setId(msg.getId());
                    ctx.writeAndFlush(response);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    responseFuture.completeExceptionally(cause);
                }
            };

            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new Http2Detector(new ChannelHandler[] {serverHandler}).getHandlers());
                        }
                    });
            ChannelFuture bindFuture = serverBootstrap.bind("127.0.0.1", 0);
            assertTrue(bindFuture.await(5, TimeUnit.SECONDS));
            assertTrue(bindFuture.isSuccess());
            serverChannel = bindFuture.channel();
            int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

            Bootstrap clientBootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(Http2FrameCodecBuilder.forClient().build())
                                    .addLast(new Http2MultiplexHandler(new ChannelDuplexHandler()));
                        }
                    });
            ChannelFuture connectFuture = clientBootstrap.connect("127.0.0.1", port);
            assertTrue(connectFuture.await(5, TimeUnit.SECONDS));
            assertTrue(connectFuture.isSuccess());
            clientChannel = connectFuture.channel();

            streamChannel = new Http2StreamChannelBootstrap(clientChannel)
                    .handler(new ChannelInitializer<Http2StreamChannel>() {
                        @Override
                        protected void initChannel(Http2StreamChannel ch) {
                            ch.pipeline()
                                    .addLast(new GrpcDecoder())
                                    .addLast(new GrpcEncoder())
                                    .addLast(new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                            if (msg instanceof RpcMessage) {
                                                responseFuture.complete((RpcMessage) msg);
                                            }
                                        }

                                        @Override
                                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                            if (evt instanceof Http2ResetFrame) {
                                                resetFrameRef.set((Http2ResetFrame) evt);
                                                responseFuture.completeExceptionally(
                                                        new AssertionError("Unexpected RST_STREAM with errorCode="
                                                                + ((Http2ResetFrame) evt).errorCode()));
                                            }
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                            responseFuture.completeExceptionally(cause);
                                        }
                                    });
                        }
                    })
                    .open()
                    .get(5, TimeUnit.SECONDS);

            ChannelFuture writeFuture = streamChannel.writeAndFlush(
                    createHeartbeatMessage(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, HeartbeatMessage.PING));
            assertTrue(writeFuture.await(5, TimeUnit.SECONDS));
            assertTrue(writeFuture.isSuccess());

            RpcMessage response = responseFuture.get(5, TimeUnit.SECONDS);
            assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE, response.getMessageType());
            assertEquals(HeartbeatMessage.PONG, response.getBody());
            assertEquals(1, response.getId());
            assertNull(resetFrameRef.get());
            assertTrue(streamChannel.isActive());
        } finally {
            if (streamChannel != null) {
                streamChannel.close().awaitUninterruptibly(2, TimeUnit.SECONDS);
            }
            if (clientChannel != null) {
                clientChannel.close().awaitUninterruptibly(2, TimeUnit.SECONDS);
            }
            if (serverChannel != null) {
                serverChannel.close().awaitUninterruptibly(2, TimeUnit.SECONDS);
            }
            group.shutdownGracefully(0, 2, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
        }
    }
}
