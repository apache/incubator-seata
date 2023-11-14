package io.seata.mock.protocol;

import io.netty.channel.Channel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.netty.AbstractNettyRemotingClient;
import io.seata.core.rpc.netty.NettyClientConfig;
import io.seata.core.rpc.netty.NettyPoolKey;
import io.seata.core.rpc.processor.client.ClientHeartbeatProcessor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

/**
 * mock client
 *
 * @author minghua.xie
 * @date 2023/11/10
 **/
public class MockNettyClient extends AbstractNettyRemotingClient {
    public MockNettyClient(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup, ThreadPoolExecutor messageExecutor, NettyPoolKey.TransactionRole transactionRole) {
        super(nettyClientConfig, eventExecutorGroup, messageExecutor, transactionRole);
    }

    @Override
    public void onRegisterMsgSuccess(String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {

    }

    @Override
    public void onRegisterMsgFail(String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {

    }

    @Override
    protected Function<String, NettyPoolKey> getPoolKeyFunction() {
        return null;
    }

    @Override
    protected String getTransactionServiceGroup() {
        return null;
    }

    @Override
    protected boolean isEnableClientBatchSendRequest() {
        return false;
    }

    @Override
    protected long getRpcRequestTimeout() {
        return 0;
    }


    private void registerProcessor() {
//        ClientHeartbeatProcessor clientHeartbeatProcessor = new ClientHeartbeatProcessor();
//        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, clientHeartbeatProcessor, null);
//
//        ;
//        super.registerProcessor(MessageType.TYPE_SEATA_MERGE_RESULT, new MockRemotingProcessor<>(), null);
//        super.registerProcessor(MessageType.TYPE_BATCH_RESULT_MSG, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT, onResponseProcessor, null);
//
//
//        super.registerProcessor(MessageType.TYPE_REG_RM_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_RM_DELETE_UNDOLOG, rmUndoLogProcessor, messageExecutor);
//        super.registerProcessor(MessageType.TYPE_BRANCH_COMMIT, rmBranchCommitProcessor, messageExecutor);
//        super.registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK, rmBranchRollbackProcessor, messageExecutor);
//        super.registerProcessor(MessageType.TYPE_BRANCH_REGISTER_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT, onResponseProcessor, null);
//
//
//        super.registerProcessor(MessageType.TYPE_GLOBAL_BEGIN_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_GLOBAL_COMMIT_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_GLOBAL_REPORT_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_GLOBAL_STATUS_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_REG_CLT_RESULT, onResponseProcessor, null);
//        super.registerProcessor(MessageType.TYPE_BATCH_RESULT_MSG, onResponseProcessor, null);
//        // 2.registry heartbeat message processor
//        ClientHeartbeatProcessor clientHeartbeatProcessor = new ClientHeartbeatProcessor();
//        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, clientHeartbeatProcessor, null);
    }
}
