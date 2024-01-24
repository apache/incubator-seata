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
package io.seata.mockserver.processor;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.Version;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock Remoting Processor
 **/
public class MockRegisterProcessor implements RemotingProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MockRegisterProcessor.class);
    private RemotingServer remotingServer;
    private Role role;

    public MockRegisterProcessor(RemotingServer remotingServer, Role role) {
        this.remotingServer = remotingServer;
        this.role = role;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        if (role == Role.TM) {
            RegisterTMRequest message = (RegisterTMRequest) rpcMessage.getBody();
            LOGGER.info("message = " + message);

            ChannelManager.registerTMChannel(message, ctx.channel());
            Version.putChannelVersion(ctx.channel(), message.getVersion());

            RegisterTMResponse resp = new RegisterTMResponse();
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resp);
            LOGGER.info("sendAsyncResponse: {}", resp);
        } else if (role == Role.RM) {
            RegisterRMRequest message = (RegisterRMRequest) rpcMessage.getBody();
            LOGGER.info("message = " + message);

            ChannelManager.registerRMChannel(message, ctx.channel());
            Version.putChannelVersion(ctx.channel(), message.getVersion());

            RegisterRMResponse resp = new RegisterRMResponse();
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resp);
            LOGGER.info("sendAsyncResponse: {}", resp);
        }
    }


    public static enum Role {
        /*
        * TM
        */
        TM,
        /**
         * RM
         */
        RM
    }
}
