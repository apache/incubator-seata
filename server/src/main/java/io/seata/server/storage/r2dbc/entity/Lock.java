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
package io.seata.server.storage.r2dbc.entity;

import static io.seata.core.model.LockStatus.Locked;

import io.seata.common.util.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * The type Lock do.
 *
 * @author jianbin.chen
 */
@Table("lock_table")
public class Lock implements Persistable {

    private String xid;

    private Long transactionId;

    private Long branchId;

    private String resourceId;

    private String tableName;

    private String pk;

    private Integer status = Locked.getCode();

    @Id
    private String rowKey;

    // true=save false=update
    @Transient
    private boolean newLock = true;


    /**
     * Instantiates a new Lock do.
     */
    public Lock() {
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public Long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets resource id.
     *
     * @return the resource id
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets resource id.
     *
     * @param resourceId the resource id
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Gets row key.
     *
     * @return the row key
     */
    public String getRowKey() {
        return rowKey;
    }

    /**
     * Sets row key.
     *
     * @param rowKey the row key
     */
    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    /**
     * Gets table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets table name.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Gets pk.
     *
     * @return the pk
     */
    public String getPk() {
        return pk;
    }

    /**
     * Sets pk.
     *
     * @param pk the pk
     */
    public void setPk(String pk) {
        this.pk = pk;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public Long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * Set status
     * @param status the status
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    @Override public Object getId() {
        return this.rowKey;
    }

    @Override
    @Transient
    public boolean isNew() {
        return this.newLock;
    }

    public boolean isNewLock() {
        return newLock;
    }

    public void setNewLock(boolean newLock) {
        this.newLock = newLock;
    }
}
