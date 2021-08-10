package io.seata.server.storage.hbase.store;

import io.seata.common.XID;
import io.seata.common.exception.StoreException;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * ClassName: HBaseTransactionStoreManager
 * Description:
 *
 * @author haishin
 */
public class HBaseTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseTransactionStoreManager.class);

    /**
     *  The HBase Table
     */
    protected String tableName = "seata:session";

    /**
     * GlobalStatus_GlobalTransactionID
     */
    protected String statusTableName = "statusTable";

    /**
     * The Global Family.
     */
    protected String globalCF = "global";

    /**
     * The Branch Family.
     */
    protected String branchesCF = "branches";

    /**
     * The Status Family
     */
    protected String transactionIdCF = "transactionId";

    private static volatile HBaseTransactionStoreManager instance;


    private static Connection connection = HBaseSingleConnectionFactory.getInstance();

    private static Table table = null;

    private static Table statusTransactionIdTable = null;

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
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(rowKey));
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
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"));
            Result result = table.get(get);
            String previousBranchStatus = Bytes.toString(CellUtil.cloneValue(result.getColumnLatestCell(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"))));
            if (StringUtils.isEmpty(previousBranchStatus)) {
                throw new StoreException("Branch transaction is not exist, update branch transaction failed.");
            }

            Date now = new Date();
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"), Bytes.toBytes(branchTransactionDO.getStatus()));
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
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Date now = new Date();
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_xid"), Bytes.toBytes(branchTransactionDO.getXid()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_branchId"), Bytes.toBytes(branchTransactionDO.getBranchId()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_transactionId"), Bytes.toBytes(branchTransactionDO.getTransactionId()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_resourceGroupId"), Bytes.toBytes(branchTransactionDO.getResourceGroupId()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_resourceId"), Bytes.toBytes(branchTransactionDO.getResourceId()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_branchType"), Bytes.toBytes(branchTransactionDO.getBranchType()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_status"), Bytes.toBytes(branchTransactionDO.getStatus()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_clientId"), Bytes.toBytes(branchTransactionDO.getClientId()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_applicationData"), Bytes.toBytes(branchTransactionDO.getApplicationData()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_gmtCreate"), Bytes.toBytes(now.toString()));
            put.addColumn(Bytes.toBytes(branchesCF), Bytes.toBytes(branchId + "_gmtModified"), Bytes.toBytes(now.toString()));

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

        try {
            statusTransactionIdTable = connection.getTable(TableName.valueOf(statusTableName));
            Delete statusDelete = new Delete(Bytes.toBytes(statusTransactionId));
            statusTransactionIdTable.delete(statusDelete);

            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            delete.addFamily(Bytes.toBytes(globalCF));
            table.delete(delete);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table,statusTransactionIdTable);
        }
    }

    private boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        Long rowKey = globalTransactionDO.getTransactionId();
        try {

            statusTransactionIdTable = connection.getTable(TableName.valueOf(statusTableName));
            int previousGlobalStatus = readSession(globalTransactionDO.getXid(),false).getStatus().getCode();
            String preStatusTransactionId = buildStatusRowKey(previousGlobalStatus, rowKey);
            Delete delete = new Delete(Bytes.toBytes(preStatusTransactionId));
            statusTransactionIdTable.delete(delete);

            table = connection.getTable(TableName.valueOf(tableName));
            Date now = new Date();
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("status"), Bytes.toBytes(globalTransactionDO.getStatus()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("gmtModified"), Bytes.toBytes(now.toString()));
            table.put(put);

            String newStatusTransactionId = buildStatusRowKey(globalTransactionDO.getStatus(), rowKey);
            Put statusPut = new Put(Bytes.toBytes(newStatusTransactionId));
            statusPut.addColumn(Bytes.toBytes(transactionIdCF), Bytes.toBytes("transactionId"), Bytes.toBytes(rowKey));
            statusTransactionIdTable.put(statusPut);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table,statusTransactionIdTable);
        }
    }

    private boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        Long rowKey = globalTransactionDO.getTransactionId();
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Date now = new Date();
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("xid"), Bytes.toBytes(globalTransactionDO.getXid()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("transactionId"), Bytes.toBytes(globalTransactionDO.getTransactionId()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("status"), Bytes.toBytes(globalTransactionDO.getStatus()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("applicationId"), Bytes.toBytes(globalTransactionDO.getApplicationId()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("transactionServiceGroup"), Bytes.toBytes(globalTransactionDO.getTransactionServiceGroup()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("transactionName"), Bytes.toBytes(globalTransactionDO.getTransactionName()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("timeout"), Bytes.toBytes(globalTransactionDO.getTimeout()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("beginTime"), Bytes.toBytes(globalTransactionDO.getBeginTime()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("applicationData"), Bytes.toBytes(globalTransactionDO.getApplicationData()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("gmtCreate"), Bytes.toBytes(now.toString()));
            put.addColumn(Bytes.toBytes(globalCF), Bytes.toBytes("gmtModified"), Bytes.toBytes(now.toString()));
            table.put(put);

            statusTransactionIdTable = connection.getTable(TableName.valueOf(statusTableName));
            String statusRowKey = buildStatusRowKey(globalTransactionDO.getStatus(), globalTransactionDO.getTransactionId());
            Put statusPut = new Put(Bytes.toBytes(statusRowKey));
            statusPut.addColumn(Bytes.toBytes(transactionIdCF), Bytes.toBytes("transactionId"), Bytes.toBytes(globalTransactionDO.getTransactionId()));
            statusTransactionIdTable.put(statusPut);

            return true;
        } catch (IOException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(table,statusTransactionIdTable);
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

        List<GlobalSession> globalSessions = Collections.synchronizedList(new ArrayList<>());
        List<Integer> statusKeys = new ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            statusKeys.add(statuses[i].getCode());
        }
        try {
            table = connection.getTable(TableName.valueOf(statusTableName));

            for (int code : statusKeys) {
                Scan scan = new Scan();
                scan.setFilter(new PrefixFilter(Bytes.toBytes(code)));
                ResultScanner resultScanner = table.getScanner(scan);
                for (Result result : resultScanner) {
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
            IOUtil.close(table);
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
        try {
            Table table = connection.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(rowKey));
            Result result = table.get(get);

            Map<String, String> map = new HashMap<String, String>();

            String branchId = null;
            boolean newBranch;
            Map<String, String> branchesMap = new HashMap<>();
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
                    if (StringUtils.isBlank(branchId)) {
                        branchId = key.split("_")[0];
                        newBranch = true;
                    } else {
                        if (!branchId.equals(key.split("_")[0]))
                            newBranch = true;
                        else
                            newBranch = false;
                    }
                    String value = branchMap.get(key);
                    key = key.split("_")[1];

                    if (newBranch) {
                        if (branchTransactionDOs != null){
                            BranchTransactionDO branchTransactionDO = (BranchTransactionDO) BeanUtils.mapToObject(branchMap, BranchTransactionDO.class);
                            branchTransactionDOs.add(branchTransactionDO);
                        }
                        branchMap.put(key, value);

                    } else {
                        branchMap.put(key, value);
                    }
                }

                BranchTransactionDO branchTransactionDO = (BranchTransactionDO) BeanUtils.mapToObject(branchMap, BranchTransactionDO.class);
                branchTransactionDOs.add(branchTransactionDO);

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
}
