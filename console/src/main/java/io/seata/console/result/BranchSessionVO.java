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
 * BranchSessionVO without convertor
 * @author: zhongxiang.wang
 * @author: Sher
 */
package io.seata.console.result;
import java.util.Objects;

public class BranchSessionVO {

    private String xid;

    private String transactionId;

    private String branchId;

    private String resourceGroupId;

    private String resourceId;

    private String branchType;

    private Integer status;

    private String clientId;

    private String applicationData;

    private Long gmtCreate;

    private Long gmtModified;


    public BranchSessionVO(){

    }

    public BranchSessionVO(String xid,
                           Long transactionId,
                           Long branchId,
                           String resourceGroupId,
                           String resourceId,
                           String branchType,
                           Integer status,
                           String clientId,
                           String applicationData) {
        this.xid = xid;
        this.transactionId = String.valueOf(transactionId);
        this.branchId = String.valueOf(branchId);
        this.resourceGroupId = resourceGroupId;
        this.resourceId = resourceId;
        this.branchType = branchType;
        this.status = status;
        this.clientId = clientId;
        this.applicationData = applicationData;
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

    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getBranchType() {
        return branchType;
    }

    public void setBranchType(String branchType) {
        this.branchType = branchType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BranchSessionVO that = (BranchSessionVO) o;
        return Objects.equals(xid, that.xid)
                && Objects.equals(transactionId, that.transactionId)
                && Objects.equals(branchId, that.branchId)
                && Objects.equals(resourceGroupId, that.resourceGroupId)
                && Objects.equals(resourceId, that.resourceId)
                && Objects.equals(branchType, that.branchType)
                && Objects.equals(status, that.status)
                && Objects.equals(clientId, that.clientId)
                && Objects.equals(applicationData, that.applicationData)
                && Objects.equals(gmtCreate, that.gmtCreate)
                && Objects.equals(gmtModified, that.gmtModified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xid,
                transactionId,
                branchId,
                resourceGroupId,
                resourceId,
                branchType,
                status,
                clientId,
                applicationData,
                gmtCreate,
                gmtModified);
    }

    @Override
    public String toString() {
        return "BranchSessionVO{" +
                "xid='" + xid + '\'' +
                ", transactionId=" + transactionId +
                ", branchId=" + branchId +
                ", resourceGroupId='" + resourceGroupId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", branchType='" + branchType + '\'' +
                ", status=" + status +
                ", clientId='" + clientId + '\'' +
                ", applicationData='" + applicationData + '\'' +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                '}';
    }
}
