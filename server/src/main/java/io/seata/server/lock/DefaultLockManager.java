package io.seata.server.lock;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.LockMode;
import io.seata.core.lock.Locker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.db.DataSourceGenerator;
import io.seata.server.session.BranchSession;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author zhangsen
 * @data 2019-05-15
 */
public class DefaultLockManager extends AbstractLockManager {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static Locker locker = null;

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        String lockKey = branchSession.getLockKey();
        if (StringUtils.isNullOrEmpty(lockKey)) {
            //no lock
            return true;
        }
        //get locks of branch
        List<RowLock> locks = collectRowLocks(branchSession);
        if(CollectionUtils.isEmpty(locks)){
            //no lock
            return true;
        }
        //TODO

    }

    @Override
    public boolean unLock(BranchSession branchSession) throws TransactionException {
        List<RowLock> locks = collectRowLocks(branchSession);
        if(CollectionUtils.isEmpty(locks)){
            //no lock
            return true;
        }
        try{
            return getLocker().unLock(CollectionUtils.toArrays(locks));
        }catch(Exception t){
            LOGGER.error("unLock error, branchSession:" + branchSession, t);
            return false;
        }
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        return false;
    }

    @Override
    public void cleanAllLocks() throws TransactionException {
        getLocker().cleanAllLocks();
    }

    public static synchronized final Locker getLocker() {
        if(locker != null){
            return locker;
        }
        String lockMode = CONFIG.getConfig(ConfigurationKeys.LOCK_MODE);
        if(LockMode.DB.name().equalsIgnoreCase(lockMode)){
            //init dataSource
            String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
            DataSourceGenerator dataSourceGenerator = EnhancedServiceLoader.load(DataSourceGenerator.class, datasourceType);
            DataSource logStoreDataSource = dataSourceGenerator.generateDataSource();
            locker = EnhancedServiceLoader.load(Locker.class, lockMode, new Object[]{logStoreDataSource});
        }else if(LockMode.MEMORY.name().equalsIgnoreCase(lockMode)){

        }else {
            locker = EnhancedServiceLoader.load(Locker.class, lockMode);
        }
        return locker;
    }
}
