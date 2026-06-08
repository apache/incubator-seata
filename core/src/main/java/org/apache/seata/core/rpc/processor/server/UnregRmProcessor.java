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
import org.apache.seata.common.Constants;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.UnregisterRMRequest;
import org.apache.seata.core.protocol.UnregisterRMResponse;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Process RM client unregister message.
 * <p>
 * process message type:
 * {@link UnregisterRMRequest}
 */
public class UnregRmProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnregRmProcessor.class);

    private RemotingServer remotingServer;

    public UnregRmProcessor(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        UnregisterRMRequest message = (UnregisterRMRequest) rpcMessage.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        boolean isSuccess = false;
        try {
            String resourceIdStr = message.getResourceIds();
            if (StringUtils.isBlank(resourceIdStr)) {
                LOGGER.warn("RM unregister request has empty resourceIds, client:{}", ipAndPort);
                UnregisterRMResponse response = new UnregisterRMResponse(false);
                remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
                return;
            }
            Set<String> resourceIdSet = new HashSet<>(Arrays.asList(resourceIdStr.split(Constants.DBKEYS_SPLIT_CHAR)));
            resourceIdSet.removeIf(StringUtils::isBlank);
            isSuccess = ChannelManager.unregisterRMChannel(ctx.channel(), resourceIdSet);
            if (isSuccess) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("RM unregister success, message:{}, channel:{}", message, ctx.channel());
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("RM unregister not successful, message:{}, channel:{}", message, ctx.channel());
                }
            }
        } catch (Exception exx) {
            LOGGER.error("RM unregister fail, client:{}, error message:{}", ipAndPort, exx.getMessage(), exx);
        }
        UnregisterRMResponse response = new UnregisterRMResponse(isSuccess);
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
    }
}
