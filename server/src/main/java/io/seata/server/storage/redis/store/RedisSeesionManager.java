package io.seata.server.storage.redis.store;

import java.util.Collection;
import java.util.List;

import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.exception.TransactionException;
import io.seata.server.session.AbstractSessionManager;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.Reloadable;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionLifecycleListener;
import io.seata.server.session.SessionManager;

/**
 * @author funkye
 * @date 2020/3/26
 */
@LoadLevel(name = "redis", scope = Scope.PROTOTYPE)
public class RedisSeesionManager  extends AbstractSessionManager
    implements SessionManager, SessionLifecycleListener, Initialize, Reloadable {

    @Override
    public void init() {

    }

    @Override
    public void reload() {

    }

    @Override
    public GlobalSession findGlobalSession(String xid) {
        return null;
    }

    @Override
    public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return null;
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return null;
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        return null;
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
        throws TransactionException {
        return null;
    }
}
