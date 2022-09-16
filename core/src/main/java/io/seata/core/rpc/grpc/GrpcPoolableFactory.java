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
package io.seata.core.rpc.grpc;

import java.net.InetSocketAddress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.rpc.RpcChannelPoolKey;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcPoolableFactory implements KeyedPoolableObjectFactory<RpcChannelPoolKey, SeataChannel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcPoolableFactory.class);

    private final AbstractGrpcRemotingClient rpcRemotingClient;

    public GrpcPoolableFactory(AbstractGrpcRemotingClient rpcRemotingClient) {
        this.rpcRemotingClient = rpcRemotingClient;
    }

    @Override
    public SeataChannel makeObject(RpcChannelPoolKey key) {
        InetSocketAddress address = NetUtil.toInetSocketAddress(key.getAddress());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[GRPC]GrpcPool create channel to " + key);
        }
        ManagedChannel managedChannel = creatNewChannel(address);
        long start = System.currentTimeMillis();
        Object response;
        GrpcClientSeataChannel channelToServer = null;
        GrpcClientSeataChannel tmpChannel = new GrpcClientSeataChannel(managedChannel, address);
        if (key.getMessage() == null) {
            throw new FrameworkException("[GRPC]register msg is null, role:" + key.getTransactionRole().name());
        }
        StreamObserver<GrpcRemoting.BiStreamMessage> clientStreamObserver = rpcRemotingClient.bindBiStream(tmpChannel);
        if (null != clientStreamObserver) {
            tmpChannel.setStreamObserver(clientStreamObserver);
        }
        try {
            response = rpcRemotingClient.sendSyncRequest(tmpChannel, key.getMessage());
            if (!isRegisterSuccess(response, key.getTransactionRole())) {
                rpcRemotingClient.onRegisterMsgFail(key.getAddress(), tmpChannel, response, key.getMessage());
            } else {
                channelToServer = tmpChannel;
                rpcRemotingClient.onRegisterMsgSuccess(key.getAddress(), tmpChannel, response, key.getMessage());
            }
        } catch (Exception exx) {
            String errMessage = exx.getMessage();
            tmpChannel.close();
            LOGGER.error("[GRPC]can not connect to server with grpc, maybe the server:{} does not support grpc protocol communication", address);
            throw new FrameworkException(
                    "register " + key.getTransactionRole().name() + " error, errMsg:" + errMessage);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[GRPC]register success, cost " + (System.currentTimeMillis() - start) + " ms, version:" + getVersion(
                    response, key.getTransactionRole()) + ",role:" + key.getTransactionRole().name() + ",channel:"
                    + channelToServer);
        }
        return channelToServer;
    }

    private boolean isRegisterSuccess(Object response, RpcChannelPoolKey.TransactionRole transactionRole) {
        if (response == null) {
            return false;
        }
        if (transactionRole.equals(RpcChannelPoolKey.TransactionRole.TMROLE)) {
            if (!(response instanceof RegisterTMResponse)) {
                return false;
            }
            RegisterTMResponse registerTMResponse = (RegisterTMResponse) response;
            return registerTMResponse.isIdentified();
        } else if (transactionRole.equals(RpcChannelPoolKey.TransactionRole.RMROLE)) {
            if (!(response instanceof RegisterRMResponse)) {
                return false;
            }
            RegisterRMResponse registerRMResponse = (RegisterRMResponse) response;
            return registerRMResponse.isIdentified();
        }
        return false;
    }

    private String getVersion(Object response, RpcChannelPoolKey.TransactionRole transactionRole) {
        if (transactionRole.equals(RpcChannelPoolKey.TransactionRole.TMROLE)) {
            return ((RegisterTMResponse) response).getVersion();
        } else {
            return ((RegisterRMResponse) response).getVersion();
        }
    }

    private ManagedChannel creatNewChannel(InetSocketAddress address) {
        return ManagedChannelBuilder
                .forAddress(address.getAddress().getHostAddress(), address.getPort())
                .usePlaintext()
                .directExecutor()
                .intercept(rpcRemotingClient.getClientInterceptors())
                .build();
    }

    @Override
    public void destroyObject(RpcChannelPoolKey key, SeataChannel channel) {
        if (channel != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("will destroy channel:" + channel);
            }
            channel.close();
        }
    }

    @Override
    public boolean validateObject(RpcChannelPoolKey key, SeataChannel obj) {
        if (obj != null && obj.isActive()) {
            return true;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("channel valid false,channel:" + obj);
        }
        return false;
    }

    @Override
    public void activateObject(RpcChannelPoolKey key, SeataChannel obj) {

    }

    @Override
    public void passivateObject(RpcChannelPoolKey key, SeataChannel obj) {

    }
}
