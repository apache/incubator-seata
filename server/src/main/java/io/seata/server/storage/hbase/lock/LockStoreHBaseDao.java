/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.storage.hbase.lock;

import io.seata.common.exception.StoreException;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.LockDO;
import io.seata.core.store.LockStore;
import io.seata.server.storage.db.lock.LockStoreDataBaseDAO;
import io.seata.server.storage.hbase.HBaseSingleConnectionFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;

import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_LOCK_KEY_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_LOCK_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_LOCK_TABLE_LOCK;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_NAMESPACE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_LOCK_KEY_TABLE_TRANSACTION;


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

    protected Connection connection = null;

    /**
     * The Column Family
     */
    protected String lockCF;

    protected String transactionIdCF;

    /**
     * the table name
     */
    protected String lockTableName;

    protected String lockKeyTableName;

    public LockStoreHBaseDao() {
        if (connection == null)
            connection = HBaseSingleConnectionFactory.getInstance();
        String namespace = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_NAMESPACE,
                DEFAULT_STORE_HBASE_NAMESPACE);
        String lockTable = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_LOCK_TABLE_NAME,
                DEFAULT_STORE_HBASE_LOCK_TABLE);
        if (StringUtils.isBlank(lockTableName))
            lockTableName = namespace + ":" + lockTable;
        if (StringUtils.isBlank(lockCF))
            lockCF = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_LOCK_TABLE_LOCK,
                    DEFAULT_STORE_HBASE_LOCK_TABLE_LOCK);
        String lockKeyTable = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_LOCK_KEY_TABLE_NAME,
                DEFAULT_STORE_HBASE_LOCK_KEY_TABLE);
        if (StringUtils.isBlank(lockKeyTableName))
            lockKeyTableName = namespace + ":" + lockKeyTable;
        if (StringUtils.isBlank(transactionIdCF))
            transactionIdCF = CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_LOCK_KEY_TABLE_TRANSACTION,
                    DEFAULT_STORE_HBASE_LOCK_KEY_TABLE_TRANSACTION);

    }

    @Override
    public boolean acquireLock(LockDO lockDO) {
        return acquireLock(Collections.singletonList(lockDO));
    }

    @Override
    public boolean acquireLock(List<LockDO> needLockDOS) {

        Set<String> dbExistedRowKeys = new HashSet<>();
        if (needLockDOS.size() > 1) {
            needLockDOS = needLockDOS.stream().
                    filter(LambdaUtils.distinctByKey(LockDO::getRowKey))
                    .collect(Collectors.toList());
        }


        String currentTransactionId = needLockDOS.get(0).getTransactionId().toString();
        boolean canLock = true;
        Table lockTable = null;
        Table lockKeyTable = null;
        try {
            lockKeyTable = connection.getTable(TableName.valueOf(lockKeyTableName));
            for (LockDO lockDO : needLockDOS) {
                Get get = new Get(Bytes.toBytes(lockDO.getRowKey()));
                Result result = lockKeyTable.get(get);

                if (result.size() > 0) {
                    String oldTransactionId = Bytes.toString(CellUtil.cloneValue(
                            result.getColumnLatestCell(Bytes.toBytes(transactionIdCF),
                                    Bytes.toBytes("transactionId"))));
                    if (!StringUtils.equals(oldTransactionId, currentTransactionId)) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("{} has been locked already!", lockDO.getRowKey());
                        }
                        canLock &= false;
                        break;
                    }
                    dbExistedRowKeys.add(lockDO.getRowKey());
                }
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), "error in querying exitedLockInfo!");
        } finally {
            IOUtil.close(lockTable, lockKeyTable);
        }

        if (!canLock) {
            return false;
        }

        List<LockDO> unrepeatedLockDOs = null;
        if (CollectionUtils.isNotEmpty(dbExistedRowKeys)) {
            unrepeatedLockDOs = needLockDOS.stream().filter(lockDO -> !dbExistedRowKeys.contains(lockDO.getRowKey()))
                    .collect(Collectors.toList());
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
                } else {
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
        Table lockTable = null;
        Table lockKeyTable = null;
        try {
            lockTable = connection.getTable(TableName.valueOf(lockTableName));
            lockKeyTable = connection.getTable(TableName.valueOf(lockKeyTableName));
            for (LockDO lockDO : lockDOs) {
                Delete delete = new Delete(Bytes.toBytes(buildRowKey(lockDO.getXid(), lockDO.getBranchId(), lockDO.getRowKey())));
                lockTable.delete(delete);

                Delete delete2 = new Delete(Bytes.toBytes(lockDO.getRowKey()));
                lockKeyTable.delete(delete2);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("error deleting locks!");
            return false;
        } finally {
            IOUtil.close(lockTable, lockKeyTable);
        }
    }


    @Override
    public boolean unLock(String xid, Long branchId) {
        Table lockTable = null;
        Table lockKeyTable = null;
        try {
            lockTable = connection.getTable(TableName.valueOf(lockTableName));
            lockKeyTable = connection.getTable(TableName.valueOf(lockKeyTableName));
            //query rowKey by xid
            List<LockDO> prepareUnLocks = new ArrayList<>();
            Scan scan = new Scan();
            readPrepareUnLocksByXidAndBranchId(xid, branchId, prepareUnLocks, scan, lockTable);

            // unlock
            for (LockDO lockDo : prepareUnLocks) {
                Delete delete = new Delete(Bytes.toBytes(buildRowKey(lockDo.getXid(), lockDo.getBranchId(), lockDo.getRowKey())));
                lockTable.delete(delete);

                Delete delete2 = new Delete(Bytes.toBytes(lockDo.getRowKey()));
                lockKeyTable.delete(delete2);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("error deleting the lock ! xid:{}, branchId:{} ", xid, branchId);
            return false;

        } finally {
            IOUtil.close(lockTable, lockKeyTable);
        }

    }

    private void readPrepareUnLocksByXidAndBranchId(String xid, Long branchId, List<LockDO> prepareUnLocks, Scan scan, Table lockTable) throws IOException {
        scan.withStartRow(Bytes.toBytes(xid + "_" + branchId));
        Filter filter = new PrefixFilter(Bytes.toBytes(xid + "_" + branchId));
        scan.setFilter(filter);
        ResultScanner resultScanner = lockTable.getScanner(scan);
        Map<String, String> lockMap = new HashMap<>();

        for (Result result : resultScanner) {
            for (Cell cell : result.rawCells()) {
                lockMap.put(Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)));
            }
            LockDO lockDO = (LockDO) BeanUtils.mapToObject(lockMap, LockDO.class);
            prepareUnLocks.add(lockDO);
            lockMap.clear();
        }
    }

    @Override
    public boolean unLock(String xid, List<Long> branchIds) {
        Table lockTable = null;
        Table lockKeyTable = null;
        try {
            lockTable = connection.getTable(TableName.valueOf(lockTableName));
            lockKeyTable = connection.getTable(TableName.valueOf(lockKeyTableName));
            //query rowKey by xid and branchId
            List<LockDO> prepareUnLocks = new ArrayList<>();
            Scan scan = new Scan();
            for (Long branchId : branchIds) {
                readPrepareUnLocksByXidAndBranchId(xid, branchId, prepareUnLocks, scan, lockTable);
            }

            // unlock
            for (LockDO lockDo : prepareUnLocks) {
                Delete delete = new Delete(Bytes.toBytes(buildRowKey(lockDo.getXid(), lockDo.getBranchId(), lockDo.getRowKey())));
                lockTable.delete(delete);

                Delete delete2 = new Delete(Bytes.toBytes(lockDo.getRowKey()));
                lockKeyTable.delete(delete2);
            }

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(lockTable, lockKeyTable);
        }
    }

    @Override
    public boolean isLockable(List<LockDO> lockDOs) {
        if (!checkLockable(lockDOs)) {
            return false;
        }
        return true;
    }

    /**
     * Whether the lock exists, if it exists, return yes, otherwise return no
     *
     * @param lockDOs
     * @return true or false
     */
    private boolean checkLockable(List<LockDO> lockDOs) {
        Table lockKeyTable = null;
        try {
            lockKeyTable = connection.getTable(TableName.valueOf(lockKeyTableName));
            for (LockDO lockDO : lockDOs) {
                Get get = new Get(Bytes.toBytes(lockDO.getRowKey()));
                Result result = lockKeyTable.get(get);
                if (result.size() > 0) {
                    String oldTransactionId = Bytes.toString(CellUtil.cloneValue(
                            result.getColumnLatestCell(Bytes.toBytes(transactionIdCF),
                                    Bytes.toBytes("transactionId"))));
                    if (!StringUtils.equals(oldTransactionId, lockDOs.get(0).getTransactionId().toString())) {
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("error in query the lock!");
        } finally {
            IOUtil.close(lockKeyTable);
        }
        return true;
    }

    protected boolean doAcquireLock(LockDO lockDO) {
        Table lockTable = null;
        Table lockKeyTable = null;
        try {
            lockTable = connection.getTable(TableName.valueOf(lockTableName));

            String rowKey = buildRowKey(lockDO.getXid(), lockDO.getBranchId(), lockDO.getRowKey());
            Put put = new Put(Bytes.toBytes(rowKey));
            if (StringUtils.isNotBlank(lockDO.getXid()))
                put.addColumn(Bytes.toBytes(lockCF), Bytes.toBytes("xid"), Bytes.toBytes(lockDO.getXid()));
            if (lockDO.getTransactionId() != 0)
                put.addColumn(Bytes.toBytes(lockCF), Bytes.toBytes("transactionId"), Bytes.toBytes(String.valueOf(lockDO.getTransactionId())));
            put.addColumn(Bytes.toBytes(lockCF), Bytes.toBytes("branchId"), Bytes.toBytes(String.valueOf(lockDO.getBranchId())));
            if (StringUtils.isNotBlank(lockDO.getResourceId()))
                put.addColumn(Bytes.toBytes(lockCF), Bytes.toBytes("resourceId"), Bytes.toBytes(lockDO.getResourceId()));
            if (StringUtils.isNotBlank(lockDO.getTableName()))
                put.addColumn(Bytes.toBytes(lockCF), Bytes.toBytes("tableName"), Bytes.toBytes(lockDO.getTableName()));
            if (StringUtils.isNotBlank(lockDO.getPk()))
                put.addColumn(Bytes.toBytes(lockCF), Bytes.toBytes("pk"), Bytes.toBytes(lockDO.getPk()));
            if (StringUtils.isNotBlank(lockDO.getRowKey()))
                put.addColumn(Bytes.toBytes(lockCF), Bytes.toBytes("rowKey"), Bytes.toBytes(lockDO.getRowKey()));

            lockTable.put(put);

            lockKeyTable = connection.getTable(TableName.valueOf(lockKeyTableName));

            Put put2 = new Put(Bytes.toBytes(lockDO.getRowKey()));
            put2.addColumn(Bytes.toBytes(transactionIdCF), Bytes.toBytes("transactionId"), Bytes.toBytes(String.valueOf(lockDO.getTransactionId())));

            lockKeyTable.put(put2);
            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(lockTable);
        }

    }

    public List<LockDO> queryLockDOs(List<LockDO> lockDOs) {
        List<LockDO> queryLocks = new ArrayList<>();
        Table lockTable = null;
        try {
            lockTable = connection.getTable(TableName.valueOf(lockTableName));
            for (LockDO lockDO : lockDOs) {
                String rowKey = buildRowKey(lockDO.getXid(), lockDO.getBranchId(), lockDO.getRowKey());
                Get get = new Get(Bytes.toBytes(rowKey));
                Result result = lockTable.get(get);
                Map<String, String> lockMap = new HashMap<>();
                if (result.size() != 0) {
                    for (Cell cell : result.rawCells()) {
                        lockMap.put(Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)));
                    }
                    queryLocks.add((LockDO) BeanUtils.mapToObject(lockMap, LockDO.class));
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), "error in query the lock data!");
        } finally {
            IOUtil.close(lockTable);
        }
        return queryLocks;
    }

    private String buildRowKey(String xid, Long branchId, String rowKey) {
        return xid + "_" + branchId + "_" + rowKey;
    }


    /**
     * only for test
     */
    public LockStoreHBaseDao(Connection connection, String lockTableName, String lockKeyTableName, String lockCF, String transactionIdCF) {
        this.connection = connection;
        this.lockTableName = lockTableName;
        this.lockCF = lockCF;
        this.lockKeyTableName = lockKeyTableName;
        this.transactionIdCF = transactionIdCF;
    }
}



