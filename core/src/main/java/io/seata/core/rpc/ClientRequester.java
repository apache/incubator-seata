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
package io.seata.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.seata.common.ConfigurationKeys;
import io.seata.common.DefaultValues;
import io.seata.common.exception.NotSupportYetException;
import io.seata.config.ConfigurationFactory;
import io.seata.core.rpc.grpc.RmGrpcRemotingClient;
import io.seata.core.rpc.grpc.TmGrpcRemotingClient;
import io.seata.core.rpc.netty.RmNettyRemotingClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;

/**
 * @author goodboycoder
 */
public class ClientRequester {
    private static volatile ClientRequester instance;

    /**
     * The functional interface type is used here instead of a specific RemotingClient instance,
     *      mainly considering that each RemotingClient is a singleton and is held and managed by its own class.
     * If it is held by the Requester, it may lead to the acquisition in some cases. to an instance of an error state.
     * For example, when the RemotingClient is closed, the expired client will be obtained through the Requester.
     */
    private final Map<RpcChannelPoolKey.TransactionRole, Supplier<RemotingClient>> remotingClientMap = new ConcurrentHashMap<>();

    private ClientRequester() {
    }

    public static ClientRequester getInstance() {
        if (null == instance) {
            synchronized (ClientRequester.class) {
                if (null == instance) {
                    instance = new ClientRequester();
                    instance.init();
                }
            }
        }
        return instance;
    }

    private void init() {
        RpcType clientRpcType = getClientRpcType();
        switch (clientRpcType) {
            case NETTY:
                register(RpcChannelPoolKey.TransactionRole.RMROLE, RmNettyRemotingClient::getInstance);
                register(RpcChannelPoolKey.TransactionRole.TMROLE, TmNettyRemotingClient::getInstance);
                break;
            case GRPC:
                register(RpcChannelPoolKey.TransactionRole.RMROLE, RmGrpcRemotingClient::getInstance);
                register(RpcChannelPoolKey.TransactionRole.TMROLE, TmGrpcRemotingClient::getInstance);
                break;
            default:
                throw new NotSupportYetException("not support rpc type:" + clientRpcType);
        }
    }

    public void register(RpcChannelPoolKey.TransactionRole role, Supplier<RemotingClient> remotingClient) {
        remotingClientMap.put(role, remotingClient);
    }


    public RmRemotingClient getRmRemotingClient() {
        Supplier<RemotingClient> remotingClientSupplier = remotingClientMap.get(RpcChannelPoolKey.TransactionRole.RMROLE);
        if (null == remotingClientSupplier) {
            throw new NotSupportYetException("No RM RemotingClient available");
        }
        return (RmRemotingClient) remotingClientSupplier.get();
    }

    public RemotingClient getTmRemotingClient() {
        Supplier<RemotingClient> remotingClientSupplier = remotingClientMap.get(RpcChannelPoolKey.TransactionRole.TMROLE);
        if (null == remotingClientSupplier) {
            throw new NotSupportYetException("No TM RemotingClient available");
        }
        return remotingClientSupplier.get();
    }

    private RpcType getClientRpcType() {
        String strRpcType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.CLIENT_RPC_TYPE, DefaultValues.DEFAULT_CLIENT_RPC_TYPE);
        return RpcType.getTypeByName(strRpcType);
    }
}
