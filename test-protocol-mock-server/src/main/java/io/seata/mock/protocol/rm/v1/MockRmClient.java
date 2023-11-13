package io.seata.mock.protocol.rm.v1;

import io.netty.util.concurrent.EventExecutorGroup;
import io.seata.core.rpc.netty.NettyClientConfig;
import io.seata.core.rpc.netty.NettyPoolKey;
import io.seata.mock.protocol.MockNettyClient;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * ?
 *
 * @author minghua.xie
 * @date 2023/11/10
 **/
public class MockRmClient extends MockNettyClient {
    public MockRmClient(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup, ThreadPoolExecutor messageExecutor, NettyPoolKey.TransactionRole transactionRole) {
        super(nettyClientConfig, eventExecutorGroup, messageExecutor, transactionRole);
    }

}
