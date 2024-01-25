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
import java.util.Set;

import org.apache.seata.core.constants.ServerTableColumnsName;

/**
 * GlobalSessionVO
 */
public class GlobalSessionVO {

    private String xid;

    private String transactionId;

    private Integer status;

    private String applicationId;

    private String transactionServiceGroup;

    private String transactionName;

    private Long timeout;

    private Long beginTime;

    private String applicationData;

    private Long gmtCreate;

    private Long gmtModified;

    private Set<BranchSessionVO> branchSessionVOs;


    public GlobalSessionVO() {

    }

    public GlobalSessionVO(String xid,
                           Long transactionId,
                           Integer status,
                           String applicationId,
                           String transactionServiceGroup,
                           String transactionName,
                           Long timeout,
                           Long beginTime,
                           String applicationData,
                           Set<BranchSessionVO> branchSessionVOs) {
        this.xid = xid;
        this.transactionId = String.valueOf(transactionId);
        this.status = status;
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.transactionName = transactionName;
        this.timeout = timeout;
        this.beginTime = beginTime;
        this.applicationData = applicationData;
        this.branchSessionVOs = branchSessionVOs;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Long beginTime) {
        this.beginTime = beginTime;
    }

    public String getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
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

    public Set<BranchSessionVO> getBranchSessionVOs() {
        return branchSessionVOs;
    }

    public void setBranchSessionVOs(Set<BranchSessionVO> branchSessionVOs) {
        this.branchSessionVOs = branchSessionVOs;
    }

    public static GlobalSessionVO convert(ResultSet rs) throws SQLException {
        GlobalSessionVO globalSessionVO = new GlobalSessionVO();
        globalSessionVO.setXid(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_XID));
        globalSessionVO.setTransactionId(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID));
        globalSessionVO.setStatus(rs.getInt(ServerTableColumnsName.GLOBAL_TABLE_STATUS));
        globalSessionVO.setApplicationId(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID));
        globalSessionVO.setTransactionServiceGroup(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP));
        globalSessionVO.setTransactionName(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME));
        globalSessionVO.setTimeout(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT));
        globalSessionVO.setBeginTime(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME));
        globalSessionVO.setApplicationData(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA));
        Timestamp gmtCreateTimestamp = rs.getTimestamp(ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE);
        if (gmtCreateTimestamp != null) {
            globalSessionVO.setGmtCreate(gmtCreateTimestamp.getTime());
        }
        Timestamp gmtModifiedTimestamp = rs.getTimestamp(ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED);
        if (gmtModifiedTimestamp != null) {
            globalSessionVO.setGmtModified(gmtModifiedTimestamp.getTime());
        }
        return globalSessionVO;
    }

    @Override
    public String toString() {
        return "GlobalSessionVO{" +
                "xid='" + xid + '\'' +
                ", transactionId=" + transactionId +
                ", status=" + status +
                ", applicationId='" + applicationId + '\'' +
                ", transactionServiceGroup='" + transactionServiceGroup + '\'' +
                ", transactionName='" + transactionName + '\'' +
                ", timeout=" + timeout +
                ", beginTime=" + beginTime +
                ", applicationData='" + applicationData + '\'' +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                ", branchSessionVOs=" + branchSessionVOs +
                '}';
    }
}
