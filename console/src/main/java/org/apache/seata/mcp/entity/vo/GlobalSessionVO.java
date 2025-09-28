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
package org.apache.seata.mcp.entity.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.seata.mcp.config.TimestampToStringDeserializer;

import java.util.Set;

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

    @JsonDeserialize(using = TimestampToStringDeserializer.class)
    private String beginTime;

    private String applicationData;

    @JsonDeserialize(using = TimestampToStringDeserializer.class)
    private String gmtCreate;

    @JsonDeserialize(using = TimestampToStringDeserializer.class)
    private String gmtModified;

    private Set<BranchSessionVO> branchSessionVOs;

    public GlobalSessionVO() {}

    public GlobalSessionVO(
            String xid,
            Long transactionId,
            Integer status,
            String applicationId,
            String transactionServiceGroup,
            String transactionName,
            Long timeout,
            String beginTime,
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

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(String gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public String getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(String gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    public Set<BranchSessionVO> getBranchSessionVOs() {
        return branchSessionVOs;
    }

    public void setBranchSessionVOs(Set<BranchSessionVO> branchSessionVOs) {
        this.branchSessionVOs = branchSessionVOs;
    }
}
