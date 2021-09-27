package io.seata.server.storage.hbase.store;

import io.seata.common.XID;
import io.seata.common.exception.StoreException;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.hbase.HBaseSingleConnectionFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_NAMESPACE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_STATUS_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_STATUS_TABLE_TRANSACTION;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_TABLE_BRANCHES;
import static io.seata.common.DefaultValues.DEFAULT_STORE_HBASE_TABLE_GLOBAL;

/**
 * ClassName: HBaseTransactionStoreManager
 *
 * @author haishin
 */
public class HBaseTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    /**
     * The constant CONFIGURATION.
     */
    protected Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     *  The HBase Table
     */
    protected String tableName;

    /**
     * The HBase statusTable
     */
    protected String statusTableName;

    /**
     * The Global Family.
     */
    protected String globalCF;

    /**
     * The Branch Family.
     */
    protected String branchesCF;

    /**
     * The StatusTable Family
     */
    protected String transactionIdCF;

    private static volatile HBaseTransactionStoreManager instance;

    protected Connection connection;

    /**
     * Get the instance.
     */
    public static HBaseTransactionStoreManager getInstance() {
        if (instance == null) {
            synchronized (HBaseTransactionStoreManager.class) {
                if (instance == null) {
                    instance = new HBaseTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    private HBaseTransactionStoreManager() {
        connection = HBaseSingleConnectionFactory.getInstance();
        String namespace = CONFIG.getConfig(ConfigurationKeys.STORE_HBASE_NAMESPACE,
                DEFAULT_STORE_HBASE_NAMESPACE);
        String table = CONFIG.getConfig(ConfigurationKeys.STORE_HBASE_TABLE_NAME,
                DEFAULT_STORE_HBASE_TABLE);
        tableName = namespace + ":" + table;
        String statusTable = CONFIG.getConfig(ConfigurationKeys.STORE_HBASE_STATUS_TABLE_NAME,
                DEFAULT_STORE_HBASE_STATUS_TABLE);
        statusTableName = namespace + ":" + statusTable;
        globalCF = CONFIG.getConfig(ConfigurationKeys.STORE_HBASE_TABLE_GLOBAL,
                DEFAULT_STORE_HBASE_TABLE_GLOBAL);
        branchesCF = CONFIG.getConfig(ConfigurationKeys.STORE_HBASE_TABLE_BRANCHES,
                DEFAULT_STORE_HBASE_TABLE_BRANCHES);
        transactionIdCF = CONFIG.getConfig(ConfigurationKeys.STORE_HBASE_STATUS_TABLE_TRANSACTION,
                DEFAULT_STORE_HBASE_STATUS_TABLE_TRANSACTION);
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {

        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return insertGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return updateGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return deleteGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return insertBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return updateBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return deleteBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    private boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        Long rowKey = branchTransactionDO.getTransactionId();
        Long branchId = branchTransactionDO.getBranchId();
        Table table = null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(rowKey.toString()));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_xid"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_branchId"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_transactionId"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_resourceGroupId"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_resourceId"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_branchType"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_clientId"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_applicationData"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_gmtCreate"));
            delete.addColumns(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_gmtModified"));

            table.delete(delete);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table);
        }

    }

    private boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        Long rowKey = branchTransactionDO.getTransactionId();
        Long branchId = branchTransactionDO.getBranchId();
        Table table = null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(rowKey.toString()));
            get.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"));
            Result result = table.get(get);
            String previousBranchStatus = Bytes.toString(CellUtil.cloneValue(result.getColumnLatestCell(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"))));
            if (StringUtils.isEmpty(previousBranchStatus)) {
                throw new StoreException("Branch transaction is not exist, update branch transaction failed.");
            }

            Date now = new Date();
            Put put = new Put(Bytes.toBytes(rowKey.toString()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"), Bytes.toBytes(String.valueOf(branchTransactionDO.getStatus())));
            if (StringUtils.isNotBlank(branchTransactionDO.getApplicationData()))
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_applicationData"), Bytes.toBytes(branchTransactionDO.getApplicationData()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_gmtModified"), Bytes.toBytes(now.toString()));
            table.put(put);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table);
        }
    }

    private boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        Long rowKey = branchTransactionDO.getTransactionId();
        Long branchId = branchTransactionDO.getBranchId();
        Table table = null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Put put = new Put(Bytes.toBytes(rowKey.toString()));
            if (StringUtils.isNotBlank(branchTransactionDO.getXid()))
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_xid"), Bytes.toBytes(branchTransactionDO.getXid()));
            if (branchTransactionDO.getBranchId() != 0)
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_branchId"), Bytes.toBytes(String.valueOf(branchTransactionDO.getBranchId())));
            if (branchTransactionDO.getTransactionId() != 0)
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_transactionId"), Bytes.toBytes(String.valueOf(branchTransactionDO.getTransactionId())));
            if (StringUtils.isNotBlank(branchTransactionDO.getResourceGroupId()))
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_resourceGroupId"), Bytes.toBytes(branchTransactionDO.getResourceGroupId()));
            if (StringUtils.isNotBlank(branchTransactionDO.getResourceId()))
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_resourceId"), Bytes.toBytes(branchTransactionDO.getResourceId()));
            if (StringUtils.isNotBlank(branchTransactionDO.getBranchType()))
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_branchType"), Bytes.toBytes(branchTransactionDO.getBranchType()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"), Bytes.toBytes(String.valueOf(branchTransactionDO.getStatus())));
            if (StringUtils.isNotBlank(branchTransactionDO.getClientId()))
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_clientId"), Bytes.toBytes(branchTransactionDO.getClientId()));
            if (StringUtils.isNotBlank(branchTransactionDO.getApplicationData()))
                put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_applicationData"), Bytes.toBytes(branchTransactionDO.getApplicationData()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_gmtCreate"), Bytes.toBytes(String.valueOf((new Date()).getTime())));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_gmtModified"), Bytes.toBytes(String.valueOf((new Date()).getTime())));

            table.put(put);
            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table);
        }
    }

    private boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        Long rowKey = globalTransactionDO.getTransactionId();
        int status = globalTransactionDO.getStatus();
        String statusTransactionId = buildStatusRowKey(status, rowKey);
        Table table = null;
        Table statusTable = null;
        try {
            statusTable = connection.getTable(TableName.valueOf(statusTableName));
            Delete statusDelete = new Delete(Bytes.toBytes(statusTransactionId));
            statusTable.delete(statusDelete);

            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(rowKey.toString()));
            delete.addFamily(Bytes.toBytes(globalCF));
            table.delete(delete);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table,statusTable);
        }
    }

    private boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        Long rowKey = globalTransactionDO.getTransactionId();
        Table table = null;
        Table statusTable = null;
        try {
            statusTable = connection.getTable(TableName.valueOf(statusTableName));
            int previousGlobalStatus = readSession(globalTransactionDO.getXid(),false).getStatus().getCode();
            String preStatusTransactionId = buildStatusRowKey(previousGlobalStatus, rowKey);
            Delete delete = new Delete(Bytes.toBytes(preStatusTransactionId));
            delete.addFamily(Bytes.toBytes(transactionIdCF));
            statusTable.delete(delete);

            table = connection.getTable(TableName.valueOf(tableName));
            Put put = new Put(Bytes.toBytes(rowKey.toString()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("status"), Bytes.toBytes(String.valueOf(globalTransactionDO.getStatus())));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("gmtModified"), Bytes.toBytes(String.valueOf(new Date().getTime())));
            table.put(put);

            String newStatusTransactionId = buildStatusRowKey(globalTransactionDO.getStatus(), rowKey);
            Put statusPut = new Put(Bytes.toBytes(newStatusTransactionId));
            statusPut.addColumn(Bytes.toBytes(transactionIdCF), Bytes.toBytes("transactionId"), Bytes.toBytes(String.valueOf(rowKey)));
            statusTable.put(statusPut);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table,statusTable);
        }
    }

    private boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String  rowKey = String.valueOf(globalTransactionDO.getTransactionId());
        Table table = null;
        Table statusTable = null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Put put = new Put(Bytes.toBytes(rowKey));
            if (StringUtils.isNotBlank(globalTransactionDO.getXid()))
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("xid"), Bytes.toBytes(globalTransactionDO.getXid()));
            if (globalTransactionDO.getTransactionId() != 0)
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("transactionId"), Bytes.toBytes(String.valueOf(globalTransactionDO.getTransactionId())));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("status"), Bytes.toBytes(String.valueOf(globalTransactionDO.getStatus())));
            if (StringUtils.isNotBlank(globalTransactionDO.getApplicationId()))
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("applicationId"), Bytes.toBytes(globalTransactionDO.getApplicationId()));
            if (StringUtils.isNotBlank(globalTransactionDO.getTransactionServiceGroup()))
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("transactionServiceGroup"), Bytes.toBytes(globalTransactionDO.getTransactionServiceGroup()));
            if (StringUtils.isNotBlank(globalTransactionDO.getTransactionName()))
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("transactionName"), Bytes.toBytes(globalTransactionDO.getTransactionName()));
            if (globalTransactionDO.getTimeout() != 0)
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("timeout"), Bytes.toBytes(String.valueOf(globalTransactionDO.getTimeout())));
            if (globalTransactionDO.getBeginTime() != 0)
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("beginTime"), Bytes.toBytes(String.valueOf(globalTransactionDO.getBeginTime())));
            if (StringUtils.isNotBlank(globalTransactionDO.getApplicationData()))
                put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("applicationData"), Bytes.toBytes(globalTransactionDO.getApplicationData()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("gmtCreate"), Bytes.toBytes(String.valueOf(new Date().getTime())));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("gmtModified"), Bytes.toBytes(String.valueOf(new Date().getTime())));
            table.put(put);

            statusTable = connection.getTable(TableName.valueOf(statusTableName));
            String statusRowKey = buildStatusRowKey(globalTransactionDO.getStatus(), globalTransactionDO.getTransactionId());
            Put statusPut = new Put(Bytes.toBytes(statusRowKey));
            statusPut.addColumn(Bytes.toBytes(transactionIdCF), Bytes.toBytes("transactionId"), Bytes.toBytes(String.valueOf(globalTransactionDO.getTransactionId())));
            statusTable.put(statusPut);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table,statusTable);
        }
    }
    
    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        List<GlobalSession> globalSessions = new ArrayList<>();
        if (StringUtils.isNotEmpty(sessionCondition.getXid())) {
            GlobalSession globalSession = this.readSession(sessionCondition.getXid(), true);
            if (globalSession != null) {
                globalSessions.add(globalSession);
            }
            return globalSessions;
        } else if (sessionCondition.getTransactionId() != null) {
            GlobalSession globalSession = this
                    .readSessionByTransactionId(sessionCondition.getTransactionId(), true);
            if (globalSession != null) {
                globalSessions.add(globalSession);
            }
            return globalSessions;
        } else if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readSession(sessionCondition.getStatuses());
        } else if (sessionCondition.getStatus() != null) {
            return readSession(new GlobalStatus[]{sessionCondition.getStatus()});
        }
        return null;
    }

    private List<GlobalSession> readSession(GlobalStatus[] statuses) {

        Table statusTable = null;
        List<GlobalSession> globalSessions = Collections.synchronizedList(new ArrayList<>());
        List<Integer> statusKeys = new ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            statusKeys.add(statuses[i].getCode());
        }
        try {
            statusTable = connection.getTable(TableName.valueOf(statusTableName));

            for (int code : statusKeys) {
                Scan scan = new Scan();
                scan.withStartRow(Bytes.toBytes(code));
                scan.setFilter(new PrefixFilter(Bytes.toBytes(code)));
                ResultScanner resultScanner = statusTable.getScanner(scan);
                Iterator<Result> results = resultScanner.iterator();
                while (results.hasNext()) {
                    Result result = results.next();
                    Long transactionId = Bytes.toLong(CellUtil.cloneValue(result.getColumnLatestCell(Bytes.toBytes(transactionIdCF), Bytes.toBytes("transactionId"))));
                    GlobalSession globalSession = this.readSessionByTransactionId(transactionId, true);
                    if (globalSession != null) {
                        globalSessions.add(globalSession);
                    }
                }
            }

            return globalSessions;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally{
            IOUtil.close(statusTable);
        }
    }

    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        Long transactionId = XID.getTransactionId(xid);
        return readSessionByTransactionId(transactionId, withBranchSessions);
    }

    @Override
    public GlobalSession readSession(String xid) { return this.readSession(xid, true); }


    public GlobalSession readSessionByTransactionId(Long transactionId, boolean withBranchSessions) {

        String rowKey = String.valueOf(transactionId);
        Table table = null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(Bytes.toBytes(globalCF));
            get.addFamily(Bytes.toBytes(branchesCF));
            Result result = table.get(get);

            Map<String, String> map = new HashMap<String, String>();

            String branchId = null;

            Map<String, String> branchesMap = new TreeMap<>();
            Map<String, String> branchMap = new HashMap<>();

            List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
            for (Cell cell: result.rawCells()) {
                String key = Bytes.toString(CellUtil.cloneQualifier(cell));
                String value = Bytes.toString(CellUtil.cloneValue(cell));
                if (key.contains("_")){
                    branchesMap.put(key, value);
                }else{
                    map.put(key,value);
                }
            }

            GlobalTransactionDO globalTransactionDO = (GlobalTransactionDO) BeanUtils.mapToObject(map, GlobalTransactionDO.class);

            if (withBranchSessions) {

                for (String key : branchesMap.keySet()) {
                    if (StringUtils.isBlank(branchId))
                        branchId = key.split("_")[0];
                    if (StringUtils.equals(branchId, key.split("_")[0])) {
                        branchMap.put(key.split("_")[1], branchesMap.get(key));
                    } else {
                        BranchTransactionDO branchTransactionDO = (BranchTransactionDO) BeanUtils.mapToObject(branchMap, BranchTransactionDO.class);
                        branchTransactionDOs.add(branchTransactionDO);
                        branchMap.clear();

                        branchMap.put(key.split("_")[1], branchesMap.get(key));
                    }
                }
                if (branchMap.size() != 0){
                    BranchTransactionDO branchTransactionDO = (BranchTransactionDO) BeanUtils.mapToObject(branchMap, BranchTransactionDO.class);
                    branchTransactionDOs.add(branchTransactionDO);
                }

            }

            return getGlobalSession(globalTransactionDO,branchTransactionDOs);

        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table);
        }


    }

    private GlobalSession getGlobalSession(GlobalTransactionDO globalTransactionDO,
                                           List<BranchTransactionDO> branchTransactionDOs) {
        GlobalSession globalSession = SessionConverter.convertGlobalSession(globalTransactionDO);
        if (CollectionUtils.isNotEmpty(branchTransactionDOs)) {
            for (BranchTransactionDO branchTransactionDO : branchTransactionDOs) {
                globalSession.add(SessionConverter.convertBranchSession(branchTransactionDO));
            }
        }
        return globalSession;
    }

    private String buildStatusRowKey(int status, long transactionId) {
        return status + "_" +transactionId;
    }

    /**
     * only for test
     */
    public void setHBaseConnection(Connection connection){ this.connection = connection; }

    /**
     * only for test
     */
    public void setTableName(String tableName) { this.tableName = tableName; }

    /**
     * only for test
     */
    public void setStatusTableName(String statusTableName) { this.statusTableName = statusTableName; }

    /**
     * only for test
     */
    public void setGlobalCF(String globalCF) { this.globalCF = globalCF; }

    /**
     * only for test
     */
    public void setBranchesCF(String branchesCF) { this.branchesCF = branchesCF; }

    /**
     * only for test
     */
    public void setTransactionIdCF(String transactionIdCF){ this.transactionIdCF = transactionIdCF; }

}
