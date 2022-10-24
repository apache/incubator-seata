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
package io.seata.rm;

import io.seata.common.ConfigurationKeys;
import io.seata.common.DefaultValues;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.config.ConfigurationFactory;
import io.seata.core.rpc.RpcType;
import io.seata.core.rpc.grpc.RmGrpcRemotingClient;
import io.seata.core.rpc.netty.RmNettyRemotingClient;

/**
 * The Rm client Initiator.
 *
 * @author slievrly
 */
public class RMClient {

    /**
     * Init.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public static void init(String applicationId, String transactionServiceGroup) {
        RpcType rpcType = getRpcType();
        switch (rpcType) {
            case NETTY:
                RmNettyRemotingClient rmNettyRemotingClient = RmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
                rmNettyRemotingClient.setResourceManager(DefaultResourceManager.get());
                rmNettyRemotingClient.setTransactionMessageHandler(DefaultRMHandler.get());
                rmNettyRemotingClient.init();
                break;
            case GRPC:
                RmGrpcRemotingClient rmGrpcRemotingClient = RmGrpcRemotingClient.getInstance(applicationId, transactionServiceGroup);
                rmGrpcRemotingClient.setResourceManager(DefaultResourceManager.get());
                rmGrpcRemotingClient.setTransactionMessageHandler(DefaultRMHandler.get());
                rmGrpcRemotingClient.init();
                break;
            default:
                throw new ShouldNeverHappenException("init RMClient fail");
        }
    }

    private static RpcType getRpcType() {
        String strRpcType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.CLIENT_RPC_TYPE, DefaultValues.DEFAULT_CLIENT_RPC_TYPE);
        RpcType rpcType = RpcType.getTypeByName(strRpcType);
        if (null == rpcType) {
            throw new RuntimeException("unknown rpc type:" + strRpcType);
        }
        return rpcType;
    }

}
