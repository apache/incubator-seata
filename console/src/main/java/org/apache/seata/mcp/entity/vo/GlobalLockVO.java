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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.mcp.utils.DateUtils;

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

    /**
     * the vgroup
     */
    private String vgroup;

    private String gmtCreate;

    private String gmtModified;

    /**
     * convert RowLock to GlobalLockVO
     * @param rowLock the RowLock
     * @return the GlobalLockVO
     */
    public static GlobalLockVO convert(RowLock rowLock, String vgroup) {
        final GlobalLockVO globalLockVO = new GlobalLockVO();
        globalLockVO.setXid(rowLock.getXid());
        globalLockVO.setTransactionId(rowLock.getTransactionId());
        globalLockVO.setBranchId(rowLock.getBranchId());
        globalLockVO.setResourceId(rowLock.getResourceId());
        globalLockVO.setTableName(rowLock.getTableName());
        globalLockVO.setPk(rowLock.getPk());
        globalLockVO.setRowKey(rowLock.getRowKey());
        globalLockVO.setVgroup(vgroup);
        return globalLockVO;
    }


    public static PageResult<GlobalLockVO> convertFromJson(ObjectMapper objectMapper, String jsonStr){
        try {
            PageResult<GlobalLockVO> result = objectMapper.readValue(jsonStr, new TypeReference<PageResult<GlobalLockVO>>() {});
            for(GlobalLockVO vo : result.getData()){
                vo.setGmtCreate(DateUtils.convertToDateTimeFromTimestamp(Long.parseLong(vo.getGmtCreate())));
                vo.setGmtModified(DateUtils.convertToDateTimeFromTimestamp(Long.parseLong(vo.getGmtModified())));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public String toString() {
        return "GlobalLockVO{" + "xid='" + xid + '\'' + ", transactionId='" + transactionId + '\'' + ", branchId='"
                + branchId + '\'' + ", resourceId='" + resourceId + '\'' + ", tableName='" + tableName + '\'' + ", pk='"
                + pk + '\'' + ", rowKey='" + rowKey + '\'' + ", vgroup='" + vgroup + '\'' + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified + '}';
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getVgroup() {
        return vgroup;
    }

    public void setVgroup(String vgroup) {
        this.vgroup = vgroup;
    }
}
