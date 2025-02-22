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
package org.apache.seata.mockserver.processor;

import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang.StringUtils;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock Remoting Processor
 **/
public class MockRegisterProcessor implements RemotingProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MockRegisterProcessor.class);


    private final RemotingServer remotingServer;
    private final Role role;


    public MockRegisterProcessor(RemotingServer remotingServer, Role role) {
        this.remotingServer = remotingServer;
        this.role = role;
    }


    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        String errorInfo = StringUtils.EMPTY;
        AbstractResultMessage response = null;
        try{
            if (role == Role.TM) {
                RegisterTMRequest message = (RegisterTMRequest) rpcMessage.getBody();
                LOGGER.info("reg message = " + message);
                ChannelManager.registerTMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                response = new RegisterTMResponse();
            } else if (role == Role.RM) {
                RegisterRMRequest message = (RegisterRMRequest) rpcMessage.getBody();
                LOGGER.info("reg message = " + message);
                ChannelManager.registerRMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                response = new RegisterRMResponse();
            }
        }catch (Exception e){
            errorInfo = e.getMessage();
            LOGGER.error(role +" register fail, error message:{}", errorInfo);
        }
        if (StringUtils.isNotEmpty(errorInfo)) {
            response.setMsg(errorInfo);
        }
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
        LOGGER.info("sendAsyncResponse: {}", response);
    }


    public enum Role {
        /**
         * The TM
         */
        TM,

        /**
         * The RM
         */
        RM
    }
}
