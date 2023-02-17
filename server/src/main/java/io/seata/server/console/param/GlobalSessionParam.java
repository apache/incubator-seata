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
package io.seata.server.console.param;

import java.io.Serializable;

import io.seata.console.param.BaseParam;

/**
 * Global session param
 * @author zhongxiang.wang
 */
public class GlobalSessionParam extends BaseParam implements Serializable {

    private static final long serialVersionUID = 115488252809011284L;
    /**
     * the xid
     */
    private String xid;
    /**
     * the application id
     */
    private String applicationId;
    /**
     * the global session status
     */
    private Integer status;
    /**
     * the transaction name
     */
    private String transactionName;
    /**
     * if with branch
     * true: with branch session
     * false: no branch session
     */
    private boolean withBranch;

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public boolean isWithBranch() {
        return withBranch;
    }

    public void setWithBranch(boolean withBranch) {
        this.withBranch = withBranch;
    }

    @Override
    public String toString() {
        return "GlobalSessionParam{" +
                "xid='" + xid + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", status=" + status +
                ", transactionName='" + transactionName + '\'' +
                ", withBranch=" + withBranch +
                '}';
    }
}
