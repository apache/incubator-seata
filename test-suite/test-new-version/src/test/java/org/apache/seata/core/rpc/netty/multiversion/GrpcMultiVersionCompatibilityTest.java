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
package org.apache.seata.core.rpc.netty.multiversion;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.Protocol;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.serializer.SerializerType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Multi-version gRPC protocol compatibility tests covering:
 * - Updated gRPC client (NettyClientBootstrap) -> legacy pre-4.2.21 gRPC server
 * - Updated gRPC client (NettyClientBootstrap) -> updated V2 gRPC server (MockNettyRemotingServer)
 * - Legacy pre-4.2.21 gRPC client -> updated V2 gRPC server (rejected due to :status request pseudo-header)
 */
public class GrpcMultiVersionCompatibilityTest extends AbstractMultiVersionCompatibilityTest {

    private static final String GRPC_SERVICE_PATH = "/org.apache.seata.protocol.protobuf.SeataService/sendRequest";

    @BeforeEach
    public void setUpGrpc() {
        useTransportProtocol(Protocol.GRPC);
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @Test
    public void testUpdatedGrpcClientToLegacyGrpcServerCompatibility() throws Exception {
        AtomicReference<Http2Headers> capturedHeaders = new AtomicReference<>();
        int port = startLegacyGrpcServer(0, capturedHeaders);

        connectV2Client("127.0.0.1", port, 5000);

        RegisterTMResponse tmResponse = doSendRegister(null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Updated gRPC client should register against legacy server");

        GlobalBeginResponse beginResponse = doSendGlobalBegin("grpc-compat-tx", 60000);
        Assertions.assertEquals(ResultCode.Success, beginResponse.getResultCode());
        Assertions.assertTrue(StringUtils.isNotBlank(beginResponse.getXid()));
        Assertions.assertEquals("grpc-compat-tx", beginResponse.getExtraData());

        doSendHeartbeatAndVerify();

        Http2Headers headers = capturedHeaders.get();
        Assertions.assertNotNull(headers, "Legacy gRPC server should receive client HTTP/2 request headers");
        Assertions.assertEquals(
                HttpMethod.POST.asciiName().toString(), headers.method().toString());
        Assertions.assertEquals(
                HttpScheme.HTTP.name().toString(), headers.scheme().toString());
        Assertions.assertEquals("127.0.0.1:" + port, headers.authority().toString());
        Assertions.assertEquals(GRPC_SERVICE_PATH, headers.path().toString());
        Assertions.assertNull(headers.status());
    }

    @Test
    public void testUpdatedGrpcClientToUpdatedV2ServerCompatibility() throws Exception {
        int port = findAvailablePort();
        startV2Server(port);
        connectV2Client("127.0.0.1", port, 5000);

        RegisterTMResponse tmResponse = doSendRegister(null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Updated gRPC client should register against updated server");

        GlobalBeginResponse beginResponse = doSendGlobalBegin("grpc-v2-tx", 60000);
        Assertions.assertEquals(ResultCode.Success, beginResponse.getResultCode());
        Assertions.assertTrue(StringUtils.isNotBlank(beginResponse.getXid()));

        doSendHeartbeatAndVerify();
    }

    @Test
    public void testLegacyGrpcClientRejectedByUpdatedV2Server() throws Exception {
        int port = findAvailablePort();
        startV2Server(port);

        AtomicReference<Http2ResetFrame> resetFrameRef = new AtomicReference<>();
        CountDownLatch resetLatch = new CountDownLatch(1);
        Http2StreamChannel streamChannel = connectLegacyGrpcClient("127.0.0.1", port, 5000, resetFrameRef, resetLatch);

        RpcMessage registerMessage = new RpcMessage();
        registerMessage.setId(getNextMessageId());
        registerMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        registerMessage.setCodec(SerializerType.GRPC.getCode());
        registerMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
        registerMessage.setBody(new RegisterTMRequest("legacyApp", "testGroup"));
        streamChannel.writeAndFlush(registerMessage);

        boolean rejected = resetLatch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(rejected, "Legacy gRPC client request with :status should be rejected by updated server");
        Assertions.assertTrue(
                resetFrameRef.get() != null || !streamChannel.isActive(),
                "Updated server should reject legacy :status request via RST_STREAM or closing the stream");
        Assertions.assertNull(responseRef.get(), "Legacy gRPC client should not receive a valid RPC response");
    }
}
