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
package io.seata.tm;

import io.seata.common.ConfigurationKeys;
import io.seata.common.DefaultValues;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.config.ConfigurationFactory;
import io.seata.core.rpc.RpcType;
import io.seata.core.rpc.grpc.TmGrpcRemotingClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;

/**
 * The type Tm client.
 *
 * @author slievrly
 */
public class TMClient {

    /**
     * Init.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public static void init(String applicationId, String transactionServiceGroup) {
        init(applicationId, transactionServiceGroup, null, null);
    }

    /**
     * Init.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param accessKey               the access key
     * @param secretKey               the secret key
     */
    public static void init(String applicationId, String transactionServiceGroup, String accessKey, String secretKey) {
        RpcType rpcType = getRpcType();
        switch (rpcType) {
            case NETTY:
                TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup, accessKey, secretKey);
                tmNettyRemotingClient.init();
                break;
            case GRPC:
                TmGrpcRemotingClient tmGrpcRemotingClient = TmGrpcRemotingClient.getInstance(applicationId, transactionServiceGroup, accessKey, secretKey);
                tmGrpcRemotingClient.init();
                break;
            default:
                throw new ShouldNeverHappenException("init TMClient fail");
        }

    }

    private static RpcType getRpcType() {
        String strRpcType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.CLIENT_RPC_TYPE, DefaultValues.DEFAULT_CLIENT_RPC_TYPE);
        return RpcType.getTypeByName(strRpcType);
    }

}
