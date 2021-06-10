package io.seata.server.storage.db.lock.distributed;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;

/**
 * @description Mysql distributedLocker
 * @author  zhongxiang.wang
 */
@LoadLevel(name = "mysql", scope = Scope.SINGLETON)
public class MysqlDistributedLocker implements DistributedLocker {

    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        return true;
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        return true;
    }
}
