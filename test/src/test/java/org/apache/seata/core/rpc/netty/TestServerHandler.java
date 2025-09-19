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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Test server handler that simulates seata server behavior
 */
public class TestServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestServerHandler.class);

    private final AtomicReference<Object> requestRef;
    private final Consumer<Object> responseCallback;

    public TestServerHandler(AtomicReference<Object> requestRef, Consumer<Object> responseCallback) {
        this.requestRef = requestRef;
        this.responseCallback = responseCallback;
    }

    public TestServerHandler() {
        this.requestRef = new AtomicReference<>();
        this.responseCallback = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        requestRef.set(msg);

        // Handle RpcMessage wrapped requests
        if (msg instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage) msg;
            Object body = rpcMessage.getBody();

            if (body instanceof RegisterTMRequest) {
                RegisterTMRequest request = (RegisterTMRequest) body;
                boolean identified = true;
                String respMsg = "";

                // Check for auth error flag (mimic real server auth logic)
                if (CodecTestCheckAuthHandler.CODEC_TEST_REG_ERROR.equals(request.getExtraData())) {
                    identified = false;
                    respMsg = "Auth Failed";
                }

                RegisterTMResponse response = new RegisterTMResponse(identified);
                response.setVersion(request.getVersion());
                response.setMsg(respMsg);

                // Wrap response in RpcMessage
                RpcMessage responseMsg = new RpcMessage();
                responseMsg.setId(rpcMessage.getId());
                responseMsg.setMessageType(ProtocolConstants.MSGTYPE_RESPONSE);
                responseMsg.setCodec(rpcMessage.getCodec());
                responseMsg.setCompressor(rpcMessage.getCompressor());
                responseMsg.setBody(response);

                ctx.writeAndFlush(responseMsg);
            }
        } else if (msg instanceof RegisterTMRequest) {
            // Handle direct requests (for backward compatibility)
            RegisterTMRequest request = (RegisterTMRequest) msg;
            boolean identified = true;

            if (CodecTestCheckAuthHandler.CODEC_TEST_REG_ERROR.equals(request.getExtraData())) {
                identified = false;
            }

            RegisterTMResponse response = new RegisterTMResponse(identified);
            response.setVersion(request.getVersion());
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
