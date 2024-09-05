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
package org.apache.seata.server.console.param;

import org.apache.seata.common.result.BaseParam;

import java.io.Serializable;


/**
 * Global lock param
 */
public class GlobalLockParam extends BaseParam implements Serializable {

    private static final long serialVersionUID = 615412528070131284L;

    /**
     * the xid
     */
    private String xid;
    /**
     * the table name
     */
    private String tableName;
    /**
     * the transaction id
     */
    private String transactionId;
    /**
     * the branch id
     */
    private String branchId;
    /**
     * the primary Key
     */
    private String pk;
    /**
     * the resourceId
     */
    private String resourceId;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
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

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "GlobalLockParam{" +
                "xid='" + xid + '\'' +
                ", tableName='" + tableName + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", branchId='" + branchId + '\'' +
                ", pk='" + pk + '\'' +
                ", resourceId='" + resourceId + '\'' +
                '}';
    }
}
