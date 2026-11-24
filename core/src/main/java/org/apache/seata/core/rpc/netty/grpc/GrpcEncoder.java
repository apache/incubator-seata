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

import com.google.protobuf.ByteString;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ServerChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.seata.core.compressor.Compressor;
import org.apache.seata.core.compressor.CompressorFactory;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcEncoder extends ChannelOutboundHandlerAdapter {
    static final String GRPC_SERVICE_PATH = "/org.apache.seata.protocol.protobuf.SeataService/sendRequest";

    private final Boolean server;
    private final String authority;
    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    public GrpcEncoder() {
        this(null, null);
    }

    public GrpcEncoder(boolean server) {
        this(Boolean.valueOf(server), null);
    }

    public GrpcEncoder(boolean server, InetSocketAddress remoteAddress) {
        this(Boolean.valueOf(server), formatAuthority(remoteAddress));
    }

    private GrpcEncoder(Boolean server, String authority) {
        this.server = server;
        this.authority = authority;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof RpcMessage)) {
            throw new UnsupportedOperationException("GrpcEncoder not support class:" + msg.getClass());
        }

        RpcMessage rpcMessage = (RpcMessage) msg;
        byte messageType = rpcMessage.getMessageType();
        Map<String, String> headMap = rpcMessage.getHeadMap();
        Object body = rpcMessage.getBody();
        int id = rpcMessage.getId();

        ChannelFuture headerFuture = null;
        if (headerSent.compareAndSet(false, true)) {
            Http2Headers headers = new DefaultHttp2Headers();
            if (isServer(ctx, messageType)) {
                headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
                headers.add(GrpcHeaderEnum.GRPC_STATUS.header, String.valueOf(0));
            } else {
                headers.method(HttpMethod.POST.asciiName());
                headers.scheme(HttpScheme.HTTP.name());
                String resolvedAuthority = resolveAuthority(ctx);
                if (resolvedAuthority != null) {
                    headers.authority(resolvedAuthority);
                }
                headers.path(GRPC_SERVICE_PATH);
            }
            headers.add(GrpcHeaderEnum.GRPC_CONTENT_TYPE.header, "application/grpc");
            headerFuture = ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers));
            if (headerFuture != null && headerFuture.isDone() && !headerFuture.isSuccess()) {
                promise.tryFailure(headerFuture.cause());
                return;
            }
        }

        ByteString dataBytes;
        if (messageType != ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST
                && messageType != ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE) {
            Serializer serializer =
                    SerializerServiceLoader.load(SerializerType.getByCode(SerializerType.GRPC.getCode()));
            byte[] serializedBytes = serializer.serialize(body);
            Compressor compressor = CompressorFactory.getCompressor(rpcMessage.getCompressor());
            dataBytes = ByteString.copyFrom(compressor.compress(serializedBytes));
        } else {
            dataBytes = ByteString.EMPTY;
        }
        headMap.put(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode()));
        headMap.put(GrpcHeaderEnum.COMPRESS_TYPE.header, String.valueOf(rpcMessage.getCompressor()));
        GrpcMessageProto.Builder builder = GrpcMessageProto.newBuilder()
                .putAllHeadMap(headMap)
                .setMessageType(messageType)
                .setId(id);
        builder.setBody(ByteString.copyFrom(dataBytes.toByteArray()));
        GrpcMessageProto grpcMessageProto = builder.build();

        byte[] bodyBytes = grpcMessageProto.toByteArray();
        byte[] messageWithPrefix = new byte[bodyBytes.length + 5];
        // The first byte is 0, indicating no compression
        messageWithPrefix[0] = 0;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(bodyBytes.length);
        byte[] lengthBytes = buffer.array();
        // The last four bytes indicate the length
        System.arraycopy(lengthBytes, 0, messageWithPrefix, 1, 4);
        // The remaining bytes are body
        System.arraycopy(bodyBytes, 0, messageWithPrefix, 5, bodyBytes.length);
        DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(messageWithPrefix));
        if (headerFuture != null && !headerFuture.isDone()) {
            ChannelPromise dataPromise = ctx.newPromise();
            combineWriteFutures(headerFuture, dataPromise, promise);
            ctx.writeAndFlush(dataFrame, dataPromise);
        } else {
            ctx.writeAndFlush(dataFrame, promise);
        }
    }

    private static void combineWriteFutures(
            ChannelFuture headerFuture, ChannelFuture dataFuture, ChannelPromise promise) {
        AtomicInteger remaining = new AtomicInteger(2);
        GenericFutureListener<Future<? super Void>> listener = future -> {
            if (!future.isSuccess()) {
                promise.tryFailure(future.cause());
            } else if (remaining.decrementAndGet() == 0) {
                promise.trySuccess();
            }
        };
        headerFuture.addListener(listener);
        dataFuture.addListener(listener);
    }

    private String resolveAuthority(ChannelHandlerContext ctx) {
        if (authority != null) {
            return authority;
        }
        Channel channel = ctx.channel();
        if (channel != null) {
            if (channel.remoteAddress() instanceof InetSocketAddress) {
                return formatAuthority((InetSocketAddress) channel.remoteAddress());
            }
            Channel parent = channel.parent();
            if (parent != null && parent.remoteAddress() instanceof InetSocketAddress) {
                return formatAuthority((InetSocketAddress) parent.remoteAddress());
            }
        }
        return null;
    }

    static String formatAuthority(InetSocketAddress address) {
        if (address == null) {
            return null;
        }
        String host = address.getHostString();
        int percentIndex = host.indexOf('%');
        if (percentIndex >= 0) {
            host = host.substring(0, percentIndex);
        }
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            return "[" + host + "]:" + address.getPort();
        }
        return host + ":" + address.getPort();
    }

    private boolean isServer(ChannelHandlerContext ctx, byte messageType) {
        if (server != null) {
            return server;
        }
        if (ctx.channel() != null) {
            Channel channel = ctx.channel();
            Channel parent = channel.parent();
            Boolean resolved = resolveServerFromChannel(parent);
            if (resolved != null) {
                return resolved;
            }
            if (parent != null && parent.parent() instanceof ServerChannel) {
                return true;
            }
            resolved = resolveServerFromChannel(channel);
            if (resolved != null) {
                return resolved;
            }
        }
        return messageType == ProtocolConstants.MSGTYPE_RESPONSE
                || messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE;
    }

    private Boolean resolveServerFromChannel(Channel channel) {
        if (channel == null || channel.pipeline() == null) {
            return null;
        }
        Http2ConnectionHandler connectionHandler = channel.pipeline().get(Http2ConnectionHandler.class);
        if (connectionHandler != null && connectionHandler.connection() != null) {
            return connectionHandler.connection().isServer();
        }
        return null;
    }
}
