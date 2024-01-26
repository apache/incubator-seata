/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.console.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.core.constants.ServerTableColumnsName;
import org.apache.seata.core.lock.RowLock;

/**
 * GlobalLockVO
 */
public class GlobalLockVO {

    private String xid;

    private String transactionId;

    private String branchId;

    private String resourceId;

    private String tableName;

    private String pk;

    private String rowKey;

    private Long gmtCreate;

    private Long gmtModified;

    /**
     * convert RowLock list to GlobalLockVO list
     * @param rowLocks the RowLock list
     * @return the GlobalLockVO list
     */
    public static List<GlobalLockVO> convert(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return Collections.emptyList();
        }
        final List<GlobalLockVO> result = new ArrayList<>(rowLocks.size());
        for (RowLock rowLock : rowLocks) {
            result.add(convert(rowLock));
        }

        return result;
    }


    /**
     * convert RowLock to GlobalLockVO
     * @param rowLock the RowLock
     * @return the GlobalLockVO
     */
    public static GlobalLockVO convert(RowLock rowLock) {
        final GlobalLockVO globalLockVO = new GlobalLockVO();
        globalLockVO.setXid(rowLock.getXid());
        globalLockVO.setTransactionId(rowLock.getTransactionId());
        globalLockVO.setBranchId(rowLock.getBranchId());
        globalLockVO.setResourceId(rowLock.getResourceId());
        globalLockVO.setTableName(rowLock.getTableName());
        globalLockVO.setPk(rowLock.getPk());
        globalLockVO.setRowKey(rowLock.getRowKey());
        return globalLockVO;
    }


    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = String.valueOf(transactionId);
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = String.valueOf(branchId);
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public Long getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Long getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Long gmtModified) {
        this.gmtModified = gmtModified;
    }

    public static GlobalLockVO convert(ResultSet rs) throws SQLException {
        GlobalLockVO globalLockVO = new GlobalLockVO();
        globalLockVO.setRowKey(rs.getString(ServerTableColumnsName.LOCK_TABLE_ROW_KEY));
        globalLockVO.setXid(rs.getString(ServerTableColumnsName.LOCK_TABLE_XID));
        globalLockVO.setTransactionId(rs.getLong(ServerTableColumnsName.LOCK_TABLE_TRANSACTION_ID));
        globalLockVO.setBranchId(rs.getLong(ServerTableColumnsName.LOCK_TABLE_BRANCH_ID));
        globalLockVO.setResourceId(rs.getString(ServerTableColumnsName.LOCK_TABLE_RESOURCE_ID));
        globalLockVO.setTableName(rs.getString(ServerTableColumnsName.LOCK_TABLE_TABLE_NAME));
        globalLockVO.setPk(rs.getString(ServerTableColumnsName.LOCK_TABLE_PK));
        Timestamp gmtCreateTimestamp = rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_CREATE);
        if (gmtCreateTimestamp != null) {
            globalLockVO.setGmtCreate(gmtCreateTimestamp.getTime());
        }
        Timestamp gmtModifiedTimestamp = rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_MODIFIED);
        if (gmtModifiedTimestamp != null) {
            globalLockVO.setGmtModified(gmtModifiedTimestamp.getTime());
        }
        return globalLockVO;
    }

    @Override
    public String toString() {
        return "GlobalLockVO{" +
                "xid='" + xid + '\'' +
                ", transactionId=" + transactionId +
                ", branchId=" + branchId +
                ", resourceId='" + resourceId + '\'' +
                ", tableName='" + tableName + '\'' +
                ", pk='" + pk + '\'' +
                ", rowKey='" + rowKey + '\'' +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                '}';
    }
}
