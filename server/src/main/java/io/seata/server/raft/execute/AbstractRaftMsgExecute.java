package io.seata.server.raft.execute;

import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.session.RaftSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jianbin.chen
 */
public abstract class AbstractRaftMsgExecute implements RaftMsgExecute<Boolean> {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected RaftSessionSyncMsg sessionSyncMsg;

    protected RaftSessionManager raftSessionManager;

    public AbstractRaftMsgExecute(RaftSessionSyncMsg sessionSyncMsg, RaftSessionManager raftSessionManager) {
        this.sessionSyncMsg = sessionSyncMsg;
        this.raftSessionManager = raftSessionManager;
    }

}
