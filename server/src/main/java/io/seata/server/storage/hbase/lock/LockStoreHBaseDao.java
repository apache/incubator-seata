package io.seata.server.storage.hbase.lock;

import io.seata.common.XID;
import io.seata.common.exception.StoreException;
import io.seata.common.util.*;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.LockDO;
import io.seata.core.store.LockStore;
import io.seata.server.storage.db.lock.LockStoreDataBaseDAO;
import io.seata.server.storage.hbase.HBaseSingleConnectionFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_NAMESPACE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_STATUS_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_STATUS_TABLE_TRANSACTION;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_TABLE_BRANCHES;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_TABLE_GLOBAL;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_TABLE_LOCK;

/**
 * ClassName: LockStoreHBaseDao
 *
 * @author haishin
 */
public class LockStoreHBaseDao implements LockStore {

    /**
     * The constant CONFIGURATION.
     */
    protected static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    private static final Logger LOGGER = LoggerFactory.getLogger(LockStoreDataBaseDAO.class);

    private static Connection connection = HBaseSingleConnectionFactory.getInstance();

    /**
     *  The HBase Table
     */
    protected String tableName;

    /**
     * The Column Family
     */
    protected String lockCF;

    private static Table table = null;

    public LockStoreHBaseDao(){
        String namespace = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_NAMESPACE,
                DEFAULT_STORE_HBASE_NAMESPACE);
        String table = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_TABLE_NAME,
                DEFAULT_STORE_HBASE_TABLE);
        tableName = namespace + ":" + table;
        String statusTable = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_STATUS_TABLE_NAME,
                DEFAULT_STORE_HBASE_STATUS_TABLE);
        lockCF = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_TABLE_LOCK,
                DEFAULT_STORE_HBASE_TABLE_LOCK);
    }

    @Override
    public boolean acquireLock(LockDO lockDO) {
        return acquireLock(Collections.singletonList(lockDO));
    }

    /**
     * @param needLockDOS
     * @return
     */
    @Override
    public boolean acquireLock(List<LockDO> needLockDOS) {
        Long transactionId = needLockDOS.get(0).getTransactionId();
        if (needLockDOS.size() > 1) {
            needLockDOS = needLockDOS.stream().
                    filter(LambdaUtils.distinctByKey(LockDO::getBranchId))
                    .collect(Collectors.toList());
        }
        List<Long> needLockKeys = new ArrayList<>();
        needLockDOS.forEach(lockDO -> needLockKeys.add(lockDO.getBranchId()));

        List<LockDO> existedLockInfos = readLocksByTransactionId(transactionId);
        String currentXID = needLockDOS.get(0).getXid();
        boolean canLock = true;
        Map<Long, LockDO> existedLockMap = new HashMap<>();
        for (LockDO lockDO : existedLockInfos) {
            existedLockMap.put(lockDO.getBranchId(), lockDO);
        }

        List<LockDO> unrepeatedLockDOs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(existedLockInfos)) {
            for (LockDO lockDO : needLockDOS) {
                if (!existedLockMap.containsKey(lockDO.getBranchId())){
                    unrepeatedLockDOs.add(lockDO);
                }
            }

        } else {
            unrepeatedLockDOs = needLockDOS;
        }

        if (CollectionUtils.isEmpty(unrepeatedLockDOs)) {
            return true;
        }
        //lock
        if (unrepeatedLockDOs.size() == 1) {
            LockDO lockDO = unrepeatedLockDOs.get(0);
            if (!doAcquireLock(lockDO)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Global lock acquire failed, xid {} branchId {} pk {}", lockDO.getXid(), lockDO.getBranchId(), lockDO.getPk());
                }
                return false;
            }
        } else {
            List<LockDO> successLocks = new ArrayList<>();
            for (LockDO lockDO : unrepeatedLockDOs) {
                if (!doAcquireLock(lockDO)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Global lock acquire failed, xid {} branchId {} pk {}", lockDO.getXid(), lockDO.getBranchId(), lockDO.getPk());
                    }
                    //rollback
                    unLock(successLocks);
                    return false;
                }else {
                    successLocks.add(lockDO);
                }
            }
        }

        return true;
    }

    @Override
    public boolean unLock(LockDO lockDO) {
        return unLock(Collections.singletonList(lockDO));
    }

    @Override
    public boolean unLock(List<LockDO> lockDOs) {
        try {
            Long transcationId = lockDOs.get(0).getTransactionId();
            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(String.valueOf(transcationId)));
            for (LockDO lockDO : lockDOs) {
                Long branchId = lockDO.getBranchId();
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_xid"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_transactionId"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_branchId"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_resourceId"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_tableName"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_pk"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_rowKey"));
            }

            table.delete(delete);
            return true;
        } catch (IOException e) {
            LOGGER.error("error deleting locks ! ");
            return false;
        } finally {
            IOUtil.close(table);
        }
    }


    @Override
    public boolean unLock(String xid, Long branchId) {
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(String.valueOf(XID.getTransactionId(xid))));
            delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_xid"));
            delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_transactionId"));
            delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_branchId"));
            delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_resourceId"));
            delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_tableName"));
            delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_pk"));
            delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_rowKey"));

            table.delete(delete);
            return true;
        } catch (IOException e) {
            LOGGER.error("error deleting the lock ! xid:{}, branchId:{} ",xid,branchId);
            return false;

        } finally {
            IOUtil.close(table);
        }

    }

    @Override
    public boolean unLock(String xid, List<Long> branchIds) {
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(String.valueOf(XID.getTransactionId(xid))));
            for (Long branchId : branchIds) {
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_xid"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_transactionId"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_branchId"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_resourceId"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_tableName"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_pk"));
                delete.addColumns(Bytes.toBytes(lockCF), Bytes.toBytes(branchId + "_rowKey"));
            }
            table.delete(delete);
            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table);
        }
    }

    @Override
    public boolean isLockable(List<LockDO> lockDOs) {
        if (!checkLockable(lockDOs)) {
            return false;
        }
        return true;
    }

    private boolean checkLockable(List<LockDO> lockDOs) {
        //query
        Long transactionId = lockDOs.get(0).getTransactionId();
        if (StringUtils.isBlank(transactionId.toString()))
            LOGGER.error("the transactionID is null!");
        List<LockDO> selectLockDOs = readLocksByTransactionId(transactionId);

        for (LockDO lockDo : selectLockDOs) {
            String xid = lockDo.getXid();
            if (!StringUtils.equals(xid, lockDOs.get(0).getXid())) {
                return false;
            }

        }
        return true;
    }

    protected boolean doAcquireLock(LockDO lockDO) {

        Long transactionId = lockDO.getTransactionId();
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Put put = new Put(Bytes.toBytes(String.valueOf(transactionId)));
            Long branchId = lockDO.getBranchId();
            if (StringUtils.isNotBlank(lockDO.getXid()))
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_xid"),Bytes.toBytes(lockDO.getXid()));
            if (lockDO.getTransactionId() != 0)
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_transactionId"),Bytes.toBytes(String.valueOf(lockDO.getTransactionId())));
            if (lockDO.getBranchId() != 0)
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_branchId"),Bytes.toBytes(String.valueOf(lockDO.getBranchId())));
            if (StringUtils.isNotBlank(lockDO.getResourceId()))
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_resourceId"),Bytes.toBytes(lockDO.getResourceId()));
            if (StringUtils.isNotBlank(lockDO.getTableName()))
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_tableName"),Bytes.toBytes(lockDO.getTableName()));
            if (StringUtils.isNotBlank(lockDO.getPk()))
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_pk"),Bytes.toBytes(lockDO.getPk()));
            if (StringUtils.isNotBlank(lockDO.getRowKey()))
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_rowKey"),Bytes.toBytes(lockDO.getRowKey()));

            table.put(put);
            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table);
        }

    }

    protected boolean doAcquireLocks( List<LockDO> lockDOs) {
        List<Put> putList = new ArrayList<>();
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            for (LockDO lockDO : lockDOs) {
                Long transactionId = lockDO.getTransactionId();
                Long branchId = lockDO.getBranchId();
                Put put = new Put(Bytes.toBytes(String.valueOf(transactionId)));
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_xid"),Bytes.toBytes(lockDO.getXid()));
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_transactionId"),Bytes.toBytes(lockDO.getTransactionId()));
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_branchId"),Bytes.toBytes(lockDO.getBranchId()));
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_resourceId"),Bytes.toBytes(lockDO.getResourceId()));
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_tableName"),Bytes.toBytes(lockDO.getTableName()));
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_pk"),Bytes.toBytes(lockDO.getPk()));
                put.addColumn(Bytes.toBytes(lockCF),Bytes.toBytes(branchId + "_rowKey"),Bytes.toBytes(lockDO.getRowKey()));

                putList.add(put);
            }
            table.put(putList);
            return true;
        } catch (IOException e) {
            LOGGER.error("Global lock batch acquire error: {}", e.getMessage(), e);
            return false;
        } finally {
            IOUtil.close(table);
        }
    }

    private List<LockDO> readLocksByTransactionId(Long transactionId) {

        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(String.valueOf(transactionId)));
            get.addFamily(Bytes.toBytes(lockCF));

            Map<String, String> locksMap = new HashMap<>();
            Result result = table.get(get);
            for (Cell cell : result.rawCells()) {
                locksMap.put(Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)));
            }

            String branchId = null;
            Map<String, String> lockMap = new HashMap<>();
            List<LockDO> selectLockDOs = new ArrayList<>();

            for (String key : locksMap.keySet()) {
                if (StringUtils.isBlank(branchId))
                    branchId = key.split("_")[0];
                if (StringUtils.equals(branchId, key.split("_")[0])) {
                    lockMap.put(key.split("_")[1], locksMap.get(key));
                } else {
                    LockDO lockDO = (LockDO) BeanUtils.mapToObject(lockMap, LockDO.class);
                    selectLockDOs.add(lockDO);

                    lockMap.put(key.split("_")[1], locksMap.get(key));
                }
            }
            if (lockMap.size() != 0){
                LockDO lockDO = (LockDO) BeanUtils.mapToObject(lockMap, LockDO.class);
                selectLockDOs.add(lockDO);
            }
            return selectLockDOs;
        } catch (IOException e) {
            throw new StoreException(e);
        }   finally {
            IOUtil.close(table);
        }
    }

}



