package com.alibaba.fescar.rm;

import com.alibaba.fescar.core.rpc.netty.RmMessageListener;
import com.alibaba.fescar.core.rpc.netty.RmRpcClient;

/**
 * The Rm client Initiator.
 *
 */
public class RMClient {

    /**
     * Init.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public static void init(String applicationId, String transactionServiceGroup) {
        RmRpcClient rmRpcClient = RmRpcClient.getInstance(applicationId, transactionServiceGroup);
        rmRpcClient.setResourceManager(DefaultResourceManager.get());
        rmRpcClient.setClientMessageListener(new RmMessageListener(DefaultRMHandler.get()));
        rmRpcClient.init();
    }

}
