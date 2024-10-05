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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import org.apache.commons.lang.StringUtils;
import org.apache.seata.core.compressor.Compressor;
import org.apache.seata.core.compressor.CompressorFactory;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

import java.util.Map;

public class GrpcDecoder extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else if (msg instanceof ReferenceCounted) {
            ReferenceCountUtil.release(msg);
        }
    }

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        ByteBuf content = msg.content();
        try {
            int readableBytes = content.readableBytes();
            byte[] bytes = new byte[readableBytes];
            content.readBytes(bytes);
            if (bytes.length < 5) {
                return;
            }

            int srcPos = 0;
            while (srcPos < readableBytes) {
                // The first byte defaults to 0, indicating that no decompression is required
                // Read the value of the next four bytes as the length of the body
                int length = ((bytes[srcPos + 1] & 0xFF) << 24) | ((bytes[srcPos + 2] & 0xFF) << 16)
                        | ((bytes[srcPos + 3] & 0xFF) << 8) | (bytes[srcPos + 4] & 0xFF);

                byte[] data = new byte[length];
                System.arraycopy(bytes, srcPos + 5, data, 0, length);
                GrpcMessageProto grpcMessageProto = GrpcMessageProto.parseFrom(data);
                byte[] bodyBytes = grpcMessageProto.getBody().toByteArray();
                int messageType = grpcMessageProto.getMessageType();
                int messageId = grpcMessageProto.getId();
                Map<String, String> headMap = grpcMessageProto.getHeadMapMap();

                RpcMessage rpcMsg = new RpcMessage();
                if (messageType <= Byte.MAX_VALUE && messageType >= Byte.MIN_VALUE) {
                    rpcMsg.setMessageType((byte) messageType);
                }
                rpcMsg.setId(messageId);
                rpcMsg.setHeadMap(grpcMessageProto.getHeadMapMap());

                if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST) {
                    rpcMsg.setBody(HeartbeatMessage.PING);
                } else if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE) {
                    rpcMsg.setBody(HeartbeatMessage.PONG);
                } else {
                    String compressType = headMap.get(GrpcHeaderEnum.COMPRESS_TYPE.header);
                    if (StringUtils.isNotBlank(compressType)) {
                        byte compress = Byte.parseByte(compressType);
                        rpcMsg.setCompressor(compress);
                        Compressor compressor = CompressorFactory.getCompressor(compress);
                        bodyBytes = compressor.decompress(bodyBytes);
                    }
                    String codecValue = headMap.get(GrpcHeaderEnum.CODEC_TYPE.header);
                    int codec = Integer.parseInt(codecValue);
                    SerializerType serializerType = SerializerType.getByCode(codec);
                    rpcMsg.setCodec(serializerType.getCode());
                    Serializer serializer = SerializerServiceLoader.load(serializerType);
                    Object messageBody = serializer.deserialize(bodyBytes);
                    rpcMsg.setBody(messageBody);
                }

                ctx.fireChannelRead(rpcMsg);

                srcPos += length + 5;
            }
        } finally {
            ReferenceCountUtil.release(content);
        }
    }


    public void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame) throws Exception {
        // TODO Subsequent decompression logic is possible
    }
}
