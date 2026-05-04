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
import org.apache.seata.core.protocol.ConnectionPoolInfo;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * process client heartbeat message request(PING).
 * <p>
 * process message type:
 * {@link HeartbeatMessage}
 *
 * @since 1.3.0
 */
public class ServerHeartbeatProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHeartbeatProcessor.class);

    private RemotingServer remotingServer;
    private final ConnectionPoolInfoCache poolInfoCache;

    public ServerHeartbeatProcessor(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
        this.poolInfoCache = new ConnectionPoolInfoCache();
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        handleStandardHeartbeat(ctx, rpcMessage);
        LOGGER.debug("received PING  {}", rpcMessage.getBody());
        HeartbeatMessage heartbeatMessage = (HeartbeatMessage) rpcMessage.getBody();
        ConnectionPoolInfo poolInfo = heartbeatMessage.getConnectionPoolInfo();
        LOGGER.debug("received PING from {}", heartbeatMessage);
        String clientAddress = ctx.channel().remoteAddress().toString();
        poolInfoCache.updatePoolInfo(clientAddress, poolInfo);
    }


    private void handleStandardHeartbeat(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        try {
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), HeartbeatMessage.PONG);
        } catch (Throwable throwable) {
            LOGGER.error("send response error: {}", throwable.getMessage(), throwable);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received PING from {}", ctx.channel().remoteAddress());
        }
    }

    public Map<String, ConnectionPoolInfo> getAllPoolInfo() {
        return poolInfoCache.getAllPoolInfo();
    }

    public ConnectionPoolInfo getClientPoolInfo(String clientAddress) {
        return poolInfoCache.getPoolInfo(clientAddress);
    }
}
