package io.seata.server.raft.execute;

import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.session.RaftSessionManager;

/**
 * @author jianbin.chen
 */
public abstract class AbstractRaftMsgExecute implements RaftMsgExecute<Boolean> {

    protected RaftSessionSyncMsg sessionSyncMsg;

    protected RaftSessionManager raftSessionManager;

    public AbstractRaftMsgExecute(RaftSessionSyncMsg sessionSyncMsg, RaftSessionManager raftSessionManager) {
        this.sessionSyncMsg = sessionSyncMsg;
        this.raftSessionManager = raftSessionManager;
    }

}
