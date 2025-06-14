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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
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

    @Test
    public void testWrite() throws Exception {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        rpcMessage.setBody(HeartbeatMessage.PING);
        rpcMessage.setId(1);
        rpcMessage.setHeadMap(new HashMap<>());

        grpcEncoder.write(ctx, rpcMessage, promise);

        verify(ctx, times(1)).writeAndFlush(any(DefaultHttp2HeadersFrame.class));
        verify(ctx, times(1)).writeAndFlush(any(DefaultHttp2DataFrame.class));
    }
}
