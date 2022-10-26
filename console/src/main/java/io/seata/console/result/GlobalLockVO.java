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

/**
 * GlobalLockVO without convertor
 * @author: zhongxiang.wang
 * @author: Sher
 */

package io.seata.console.result;


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
