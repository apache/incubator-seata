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
package org.apache.seata.core.rpc.processor.server;

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RegisterCheckAuthHandler;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process RM client registry message.
 * <p>
 * process message type:
 * {@link RegisterRMRequest}
 *
 * @since 1.3.0
 */
public class RegRmProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegRmProcessor.class);

    private RemotingServer remotingServer;

    private RegisterCheckAuthHandler checkAuthHandler;

    public RegRmProcessor(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
        this.checkAuthHandler = EnhancedServiceLoader.load(RegisterCheckAuthHandler.class);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        onRegRmMessage(ctx, rpcMessage);
    }

    private void onRegRmMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        RegisterRMRequest message = (RegisterRMRequest) rpcMessage.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        boolean isSuccess = false;
        String errorInfo = StringUtils.EMPTY;
        try {
            if (null == checkAuthHandler || checkAuthHandler.regResourceManagerCheckAuth(message)) {
                ChannelManager.registerRMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                isSuccess = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("RM checkAuth for client:{},vgroup:{},applicationId:{} is OK", ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("RM checkAuth for client:{},vgroup:{},applicationId:{} is FAIL", ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            }
        } catch (Exception exx) {
            isSuccess = false;
            errorInfo = exx.getMessage();
            LOGGER.error("RM register fail, error message:{}", errorInfo);
        }
        RegisterRMResponse response = new RegisterRMResponse(isSuccess);
        if (StringUtils.isNotEmpty(errorInfo)) {
            response.setMsg(errorInfo);
        }
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
        if (isSuccess && LOGGER.isInfoEnabled()) {
            LOGGER.info("RM register success,message:{},channel:{},client version:{}", message, ctx.channel(),
                message.getVersion());
        }
    }

}
