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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class GrpcDecoderTest {
    private GrpcDecoder grpcDecoder;

    @Mock
    private ChannelHandlerContext ctx;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        grpcDecoder = new GrpcDecoder();
    }

    private byte[] createMessageBytes(GrpcMessageProto proto) {
        byte[] data = proto.toByteArray();
        byte[] lengthBytes = new byte[] {0, 0, 0, 0, (byte) data.length};
        byte[] messageBytes = new byte[lengthBytes.length + data.length];
        System.arraycopy(lengthBytes, 0, messageBytes, 0, lengthBytes.length);
        System.arraycopy(data, 0, messageBytes, lengthBytes.length, data.length);
        return messageBytes;
    }

    private GrpcMessageProto createHeartbeatRequestProto() {
        return GrpcMessageProto.newBuilder()
                .setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST)
                .setId(1)
                .build();
    }

    private void verifyRpcMessage(RpcMessage rpcMessage) {
        assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, rpcMessage.getMessageType());
        assertEquals(1, rpcMessage.getId());
        assertEquals(HeartbeatMessage.PING, rpcMessage.getBody());
    }

    @Test
    public void testOnDataRead() throws Exception {
        GrpcMessageProto proto = createHeartbeatRequestProto();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(createMessageBytes(proto));
        Http2DataFrame dataFrame = new DefaultHttp2DataFrame(byteBuf);

        grpcDecoder.onDataRead(ctx, dataFrame);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ctx).fireChannelRead(captor.capture());
        verifyRpcMessage((RpcMessage) captor.getValue());
    }

    @Test
    public void testChannelRead() throws Exception {
        GrpcMessageProto proto = createHeartbeatRequestProto();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(createMessageBytes(proto));
        Http2DataFrame dataFrame = new DefaultHttp2DataFrame(byteBuf, true);

        try {
            grpcDecoder.channelRead(ctx, dataFrame);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(ctx).fireChannelRead(captor.capture());
            verifyRpcMessage((RpcMessage) captor.getValue());
        } finally {
            if (dataFrame.refCnt() > 0) {
                dataFrame.release();
            }
        }
    }
}
