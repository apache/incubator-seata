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
import org.apache.seata.core.compressor.Compressor;
import org.apache.seata.core.compressor.CompressorFactory;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GrpcEncoder extends ChannelOutboundHandlerAdapter {
    private final Boolean server;
    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    public GrpcEncoder() {
        this.server = null;
    }

    public GrpcEncoder(boolean server) {
        this.server = server;
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

        if (headerSent.compareAndSet(false, true)) {
            Http2Headers headers = new DefaultHttp2Headers();
            if (isServer(ctx, messageType)) {
                headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
                headers.add(GrpcHeaderEnum.GRPC_STATUS.header, String.valueOf(0));
            } else {
                headers.method(HttpMethod.POST.asciiName());
                headers.scheme(HttpScheme.HTTP.name());
                headers.path("/");
            }
            headers.add(GrpcHeaderEnum.GRPC_CONTENT_TYPE.header, "application/grpc");
            ChannelFuture headerFuture = ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers));
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
        ctx.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(messageWithPrefix)), promise);
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
