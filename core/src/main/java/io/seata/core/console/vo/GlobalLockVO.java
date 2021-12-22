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
package io.seata.core.console.vo;

import io.seata.core.constants.ServerTableColumnsName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * GlobalLockVO
 * @author: zhongxiang.wang
 */
public class GlobalLockVO {

    private String xid;

    private Long transactionId;

    private Long branchId;

    private String resourceId;

    private String tableName;

    private String pk;

    private String rowKey;

    private Date gmtCreate;

    private Date gmtModified;


    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
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

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
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
        globalLockVO.setGmtCreate(rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_CREATE));
        globalLockVO.setGmtModified(rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_MODIFIED));
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
