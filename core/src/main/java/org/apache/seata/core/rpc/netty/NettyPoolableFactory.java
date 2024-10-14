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

import io.netty.channel.Channel;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.auth.JwtAuthManager;
import org.apache.seata.core.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * The type Netty key poolable factory.
 *
 */
public class NettyPoolableFactory implements KeyedPoolableObjectFactory<NettyPoolKey, Channel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyPoolableFactory.class);

    private final AbstractNettyRemotingClient rpcRemotingClient;

    private final NettyClientBootstrap clientBootstrap;

    /**
     * Instantiates a new Netty key poolable factory.
     *
     * @param rpcRemotingClient the rpc remoting client
     */
    public NettyPoolableFactory(AbstractNettyRemotingClient rpcRemotingClient, NettyClientBootstrap clientBootstrap) {
        this.rpcRemotingClient = rpcRemotingClient;
        this.clientBootstrap = clientBootstrap;
    }

    @Override
    public Channel makeObject(NettyPoolKey key) {
        InetSocketAddress address = NetUtil.toInetSocketAddress(key.getAddress());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("NettyPool create channel to " + key);
        }
        Channel tmpChannel = clientBootstrap.getNewChannel(address);
        long start = System.currentTimeMillis();
        Object response;
        Channel channelToServer = null;
        if (key.getMessage() == null) {
            throw new FrameworkException("register msg is null, role:" + key.getTransactionRole().name());
        }
        try {
            response = rpcRemotingClient.sendSyncRequest(tmpChannel, key.getMessage());

            if ((key.getTransactionRole().equals(NettyPoolKey.TransactionRole.TMROLE)
                    && !(response instanceof RegisterTMResponse))
                    || (key.getTransactionRole().equals(NettyPoolKey.TransactionRole.RMROLE)
                    && !(response instanceof RegisterRMResponse))) {
                rpcRemotingClient.onRegisterMsgFail(key.getAddress(), tmpChannel, response, key.getMessage());
            }

            if (((AbstractIdentifyResponse) response).getResultCode().equals(ResultCode.AccessTokenExpired)) {
                // refresh token to get access token
                JwtAuthManager.getInstance().setAccessToken(null);
                AbstractIdentifyRequest request = (AbstractIdentifyRequest)key.getMessage();
                String identifyExtraData = JwtAuthManager.getInstance().getAuthData();
                request.setExtraData(identifyExtraData);
                response = rpcRemotingClient.sendSyncRequest(tmpChannel, request);
            }
            if (((AbstractIdentifyResponse) response).getResultCode().equals(ResultCode.RefreshTokenExpired)) {
                // relogin to get refresh token and access token
                JwtAuthManager.getInstance().setAccessToken(null);
                JwtAuthManager.getInstance().setRefreshToken(null);
                AbstractIdentifyRequest request = (AbstractIdentifyRequest)key.getMessage();
                String identifyExtraData = JwtAuthManager.getInstance().getAuthData();
                request.setExtraData(identifyExtraData);
                response = rpcRemotingClient.sendSyncRequest(tmpChannel, request);
            }
            ResultCode resultCode = ((AbstractIdentifyResponse) response).getResultCode();
            if (resultCode.equals(ResultCode.AccessTokenNearExpiration)) {
                // access token near expiration
                JwtAuthManager.getInstance().setAccessTokenNearExpiration(true);
                channelToServer = tmpChannel;
                rpcRemotingClient.onRegisterMsgSuccess(key.getAddress(), tmpChannel, response, key.getMessage());
                rpcRemotingClient.getClientChannelManager().registerChannel(key.getAddress(), tmpChannel);
            } else if (resultCode.equals(ResultCode.Success)) {
                channelToServer = tmpChannel;
                rpcRemotingClient.onRegisterMsgSuccess(key.getAddress(), tmpChannel, response, key.getMessage());
                rpcRemotingClient.getClientChannelManager().registerChannel(key.getAddress(), tmpChannel);
            } else {
                rpcRemotingClient.onRegisterMsgFail(key.getAddress(), tmpChannel, response, key.getMessage());
            }
        } catch (Exception exx) {
            if (tmpChannel != null) {
                tmpChannel.close();
            }
            throw new FrameworkException(
                    "register " + key.getTransactionRole().name() + " error, errMsg:" + exx.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("register success, cost " + (System.currentTimeMillis() - start) + " ms, version:" + getVersion(
                    response, key.getTransactionRole()) + ",role:" + key.getTransactionRole().name() + ",channel:"
                    + channelToServer);
        }
        return channelToServer;
    }

    private String getVersion(Object response, NettyPoolKey.TransactionRole transactionRole) {
        if (transactionRole.equals(NettyPoolKey.TransactionRole.TMROLE)) {
            return ((RegisterTMResponse) response).getVersion();
        } else {
            return ((RegisterRMResponse) response).getVersion();
        }
    }

    @Override
    public void destroyObject(NettyPoolKey key, Channel channel) throws Exception {
        if (channel != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("will destroy channel:" + channel);
            }
            channel.disconnect();
            channel.close();
        }
    }

    @Override
    public boolean validateObject(NettyPoolKey key, Channel obj) {
        if (obj != null && obj.isActive()) {
            return true;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("channel valid false,channel:" + obj);
        }
        return false;
    }

    @Override
    public void activateObject(NettyPoolKey key, Channel obj) throws Exception {

    }

    @Override
    public void passivateObject(NettyPoolKey key, Channel obj) throws Exception {

    }
}
