package io.seata.mock.protocol.tm.v1;

import io.netty.channel.Channel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.rpc.netty.AbstractNettyRemotingClient;
import io.seata.core.rpc.netty.NettyClientConfig;
import io.seata.core.rpc.netty.NettyPoolKey;
import io.seata.mock.protocol.MockNettyClient;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

/**
 * ?
 *
 * @author minghua.xie
 * @date 2023/11/10
 **/
public class MockTmClient extends MockNettyClient {
    public MockTmClient(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup, ThreadPoolExecutor messageExecutor, NettyPoolKey.TransactionRole transactionRole) {
        super(nettyClientConfig, eventExecutorGroup, messageExecutor, transactionRole);
    }


}
