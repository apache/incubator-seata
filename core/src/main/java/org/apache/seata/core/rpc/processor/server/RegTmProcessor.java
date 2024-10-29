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
import org.apache.seata.core.auth.AuthResult;
import org.apache.seata.core.protocol.*;
import org.apache.seata.core.rpc.RegisterCheckAuthHandler;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process TM client registry message.
 * <p>
 * process message type:
 * {@link RegisterTMRequest}
 *
 * @since 1.3.0
 */
public class RegTmProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegTmProcessor.class);

    private RemotingServer remotingServer;

    private RegisterCheckAuthHandler checkAuthHandler;

    public RegTmProcessor(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
        this.checkAuthHandler = EnhancedServiceLoader.load(RegisterCheckAuthHandler.class);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        onRegTmMessage(ctx, rpcMessage);
    }

    private void onRegTmMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        RegisterTMRequest message = (RegisterTMRequest) rpcMessage.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        Version.putChannelVersion(ctx.channel(), message.getVersion());
        RegisterTMResponse response = new RegisterTMResponse(false);
        try {
            AuthResult authResult = (checkAuthHandler != null) ? checkAuthHandler.regTransactionManagerCheckAuth(message) : null;
            if (checkAuthHandler == null || authResult.getResultCode().equals(ResultCode.Success)
                    || authResult.getResultCode().equals(ResultCode.AccessTokenNearExpiration)) {
                ChannelManager.registerTMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                response.setIdentified(true);
                response.setResultCode(checkAuthHandler == null ? ResultCode.Success : authResult.getResultCode());
                response.setExtraData(checkAuthHandler.fetchNewToken(authResult));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("TM checkAuth for client:{},vgroup:{},applicationId:{} is OK",
                            ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            } else {
                if (authResult.getResultCode().equals(ResultCode.Failed)) {
                    response.setMsg("TM checkAuth failed!Please check your username/password.");
                } else if (authResult.getResultCode().equals(ResultCode.AccessTokenExpired)) {
                    response.setMsg("TM checkAuth failed! The access token has been expired.");
                } else if (authResult.getResultCode().equals(ResultCode.RefreshTokenExpired)) {
                    response.setMsg("TM checkAuth failed! The refresh token has been expired.");
                }
                response.setResultCode(authResult.getResultCode());
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("TM checkAuth for client:{},vgroup:{},applicationId:{} is FAIL",
                            ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            }
        } catch (IncompatibleVersionException e) {
            LOGGER.error("TM register fail, error message:{}", e.getMessage());
            response.setResultCode(ResultCode.Failed);
        }
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
        if (response.isIdentified() && LOGGER.isInfoEnabled()) {
            LOGGER.info("TM register success,message:{},channel:{},client version:{}", message, ctx.channel(),
                    message.getVersion());
        }
    }

}
