package io.seata.server.storage.db.lock.distributed;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;

/**
 * @description Postgresql distributedLocker
 * @author zhongxiang.wang
 */
@LoadLevel(name = "postgresql", scope = Scope.SINGLETON)
public class PostgresqlDistributedLocker implements DistributedLocker {

    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        return true;
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        return true;
    }
}
