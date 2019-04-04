package com.alibaba.fescar.core.store.db;

import com.alibaba.fescar.common.loader.LoadLevel;
import com.alibaba.fescar.core.store.BranchTransactionDO;
import com.alibaba.fescar.core.store.GlobalTransactionDO;
import com.alibaba.fescar.core.store.LogStore;

import javax.sql.DataSource;

/**
 * The type Log store data base dao.
 *
 * @author zhangsen
 * @data 2019/4/2
 */
@LoadLevel(name = "db")
public class LogStoreDataBaseDAO implements LogStore {

    /**
     * The Log store data source.
     */
    protected DataSource logStoreDataSource = null;

    /**
     * Instantiates a new Log store data base dao.
     */
    public LogStoreDataBaseDAO(){
    }

    /**
     * Instantiates a new Log store data base dao.
     *
     * @param logStoreDataSource the log store data source
     */
    public LogStoreDataBaseDAO(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
    }

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(String xid) {
        return null;
    }

    @Override
    public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        return false;
    }

    @Override
    public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        return false;
    }

    @Override
    public boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        return false;
    }

    @Override
    public boolean queryBranchTransactionDO(String xid) {
        return false;
    }

    @Override
    public boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        return false;
    }

    @Override
    public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        return false;
    }

    @Override
    public boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        return false;
    }

    /**
     * Sets log store data source.
     *
     * @param logStoreDataSource the log store data source
     */
    public void setLogStoreDataSource(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
    }

}
