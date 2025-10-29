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
package org.apache.seata.core.rpc.netty.v0;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.serializer.SerializerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProtocolEncoderV0Test {

    private ProtocolEncoderV0 encoder;

    @Mock
    private ChannelHandlerContext ctx;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        encoder = new ProtocolEncoderV0();
    }

    @Test
    public void testConstructor() {
        assertNotNull(encoder);
    }

    @Test
    public void testEncodeHeartbeatRequest() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        rpcMessage.setBody(HeartbeatMessage.PING);
        rpcMessage.setCodec(SerializerType.SEATA.getCode());

        ByteBuf out = Unpooled.buffer();
        encoder.encode(rpcMessage, out);

        // Verify magic code
        assertEquals(ProtocolConstantsV0.MAGIC, out.readShort());

        // Verify flags
        // Note: Heartbeat messages don't have REQUEST flag set in v0 protocol
        // because ProtocolRpcMessageV0.isRequest() only checks for MSGTYPE_RESQUEST_ONEWAY/SYNC
        short flags = out.readShort();
        assertEquals(ProtocolConstantsV0.FLAG_HEARTBEAT | ProtocolConstantsV0.FLAG_SEATA_CODEC, flags);

        // Verify body length (should be 0 for heartbeat)
        assertEquals(0, out.readShort());

        // Verify message id
        assertEquals(1L, out.readLong());

        // No body bytes for heartbeat
        assertEquals(0, out.readableBytes());

        out.release();
    }

    @Test
    public void testEncodeHeartbeatResponse() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(2);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE);
        rpcMessage.setBody(HeartbeatMessage.PONG);
        rpcMessage.setCodec(SerializerType.SEATA.getCode());

        ByteBuf out = Unpooled.buffer();
        encoder.encode(rpcMessage, out);

        // Verify magic code
        assertEquals(ProtocolConstantsV0.MAGIC, out.readShort());

        // Verify flags (heartbeat but not request)
        short flags = out.readShort();
        assertEquals(ProtocolConstantsV0.FLAG_HEARTBEAT | ProtocolConstantsV0.FLAG_SEATA_CODEC, flags);

        // Verify body length (should be 0 for heartbeat)
        assertEquals(0, out.readShort());

        // Verify message id
        assertEquals(2L, out.readLong());

        // No body bytes for heartbeat
        assertEquals(0, out.readableBytes());

        out.release();
    }

    @Test
    public void testEncodeNormalRequestWithSeataCodec() {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setResourceIds("test-resource");
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(3);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        rpcMessage.setBody(request);
        rpcMessage.setCodec(SerializerType.SEATA.getCode());

        ByteBuf out = Unpooled.buffer();
        encoder.encode(rpcMessage, out);

        // Verify basic structure was encoded
        // Magic code
        assertEquals(ProtocolConstantsV0.MAGIC, out.readShort());

        // Verify flags
        short flags = out.readShort();
        assertEquals(ProtocolConstantsV0.FLAG_REQUEST | ProtocolConstantsV0.FLAG_SEATA_CODEC, flags);

        out.release();
    }

    @Test
    public void testEncodeNormalRequestWithHessianCodec() {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setResourceIds("test-resource");
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(4);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        rpcMessage.setBody(request);
        rpcMessage.setCodec(SerializerType.HESSIAN.getCode());

        ByteBuf out = Unpooled.buffer();
        encoder.encode(rpcMessage, out);

        // Verify basic structure was encoded
        // Magic code
        assertEquals(ProtocolConstantsV0.MAGIC, out.readShort());

        // Verify flags (no SEATA_CODEC flag)
        short flags = out.readShort();
        assertEquals(ProtocolConstantsV0.FLAG_REQUEST, flags);

        out.release();
    }

    @Test
    public void testEncodeResponse() {
        RegisterRMResponse response = new RegisterRMResponse();
        response.setIdentified(true);
        response.setVersion("1.0");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(5);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESPONSE);
        rpcMessage.setBody(response);
        rpcMessage.setCodec(SerializerType.SEATA.getCode());

        ByteBuf out = Unpooled.buffer();
        encoder.encode(rpcMessage, out);

        // Verify basic structure was encoded
        // Magic code
        assertEquals(ProtocolConstantsV0.MAGIC, out.readShort());

        // Verify flags (no REQUEST flag for response)
        short flags = out.readShort();
        assertEquals(ProtocolConstantsV0.FLAG_SEATA_CODEC, flags);

        out.release();
    }

    @Test
    public void testEncodeAsyncRequest() {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setResourceIds("test-resource");
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(6);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        rpcMessage.setBody(request);
        rpcMessage.setCodec(SerializerType.SEATA.getCode());

        ByteBuf out = Unpooled.buffer();
        encoder.encode(rpcMessage, out);

        // Verify magic code
        assertEquals(ProtocolConstantsV0.MAGIC, out.readShort());

        // Verify flags include REQUEST and SEATA_CODEC
        short flags = out.readShort();
        assertEquals(true, (flags & ProtocolConstantsV0.FLAG_REQUEST) > 0);
        assertEquals(true, (flags & ProtocolConstantsV0.FLAG_SEATA_CODEC) > 0);

        out.release();
    }

    @Test
    public void testEncodeWithChannelHandlerContext() throws Exception {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(7);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        rpcMessage.setBody(HeartbeatMessage.PING);
        rpcMessage.setCodec(SerializerType.SEATA.getCode());

        ByteBuf out = Unpooled.buffer();
        encoder.encode(ctx, rpcMessage, out);

        // Verify message was encoded
        assertEquals(true, out.readableBytes() > 0);

        // Verify magic code
        assertEquals(ProtocolConstantsV0.MAGIC, out.readShort());

        out.release();
    }

    @Test
    public void testEncodeUnsupportedMessageType() throws Exception {
        String unsupportedMsg = "This is not an RpcMessage";

        ByteBuf out = Unpooled.buffer();
        encoder.encode(ctx, unsupportedMsg, out);

        // Should handle gracefully, no exception thrown but error logged
        // The encoder catches exceptions and logs them
        out.release();
    }

    @Test
    public void testEncodeRequestFlagsCorrectly() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        rpcMessage.setBody(new RegisterRMRequest());
        rpcMessage.setCodec(SerializerType.SEATA.getCode());

        ByteBuf encoded = Unpooled.buffer();
        encoder.encode(rpcMessage, encoded);

        // Verify we can read the encoded header
        assertEquals(ProtocolConstantsV0.MAGIC, encoded.readShort());
        short flags = encoded.readShort();
        // Request sync messages should have REQUEST flag
        assertEquals(true, (flags & ProtocolConstantsV0.FLAG_REQUEST) > 0);
        assertEquals(true, (flags & ProtocolConstantsV0.FLAG_SEATA_CODEC) > 0);

        encoded.release();
    }
}
