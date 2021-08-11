package io.seata.server.storage.elasticsearch.lock;

import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author UmizzZ
 * @date
 */
@LoadLevel(name="elasticsearch")
public class ElasticSearchLockManager extends AbstractLockManager implements Initialize {

    private Locker locker;

    @Override
    public void init(){
        locker = new ElasticSearchLocker();
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        try{
            return getLocker().releaseLock(branchSession.getXid(), branchSession.getBranchId());
        } catch (Exception t){
            LOGGER.error("unlock err, xid {}, branchId:{}", branchSession.getXid(), branchSession.getBranchId(), t);
            return false;
        }
    }

    @Override
    public Locker getLocker(BranchSession branchSession){
        return locker;
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        List<BranchSession> branchSessions = globalSession.getBranchSessions();
        if (CollectionUtils.isEmpty(branchSessions)){
            return true;
        }
        List<Long> branchIds = branchSessions.stream().map(BranchSession::getBranchId).collect(Collectors.toList());
        try{
            return getLocker().releaseLock(globalSession.getXid(), branchIds);
        }
        catch(Exception t){
            LOGGER.error("unlock globalSession error, xid:{} branchIds:{}", globalSession.getXid(), CollectionUtils.toString(branchIds), t);
            return false;
        }
    }
}
