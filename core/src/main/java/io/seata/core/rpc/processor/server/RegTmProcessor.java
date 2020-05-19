/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.rpc.processor.server;

import io.netty.channel.ChannelHandlerContext;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.Version;
import io.seata.core.rpc.ChannelManager;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.netty.RegisterCheckAuthHandler;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process TM client registry message.
 * <p>
 * message type:
 * {@link RegisterTMRequest}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class RegTmProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegTmProcessor.class);

    private RemotingServer remotingServer;

    private RegisterCheckAuthHandler checkAuthHandler;

    public RegTmProcessor(RemotingServer remotingServer, RegisterCheckAuthHandler checkAuthHandler) {
        this.remotingServer = remotingServer;
        this.checkAuthHandler = checkAuthHandler;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        onRegTmMessage(ctx, rpcMessage);
    }

    private void onRegTmMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        RegisterTMRequest message = (RegisterTMRequest) rpcMessage.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        Version.putChannelVersion(ctx.channel(), message.getVersion());
        boolean isSuccess = false;
        try {
            if (null == checkAuthHandler || checkAuthHandler.regTransactionManagerCheckAuth(message)) {
                ChannelManager.registerTMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                isSuccess = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("checkAuth for client:{},vgroup:{},applicationId:{}",
                        ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            }
        } catch (Exception exx) {
            isSuccess = false;
            LOGGER.error(exx.getMessage());
        }
        remotingServer.sendResponse(rpcMessage, ctx.channel(), new RegisterTMResponse(isSuccess));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("TM register success,message:{},channel:{}", message, ctx.channel());
        }
    }

}
