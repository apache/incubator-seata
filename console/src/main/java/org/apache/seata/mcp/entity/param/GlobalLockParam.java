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
package org.apache.seata.mcp.entity.param;

import org.apache.seata.common.util.PageUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.dto.GlobalLockParamDto;
import org.apache.seata.mcp.utils.DateUtils;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * Global lock param
 */
public class GlobalLockParam implements Serializable {

    private static final long serialVersionUID = 615412528070131284L;

    private String xid;

    private String tableName;

    private String transactionId;

    private String branchId;

    private String pk;

    private String resourceId;

    private int pageNum;

    private int pageSize;

    private Long timeStart;

    private Long timeEnd;

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Long timeEnd) {
        this.timeEnd = timeEnd;
    }

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

    public static GlobalLockParam convertFromParamDto(GlobalLockParamDto paramDto) {
        PageUtil.checkParam(paramDto.getPageNum(), paramDto.getPageSize());
        GlobalLockParam param = new GlobalLockParam();
        BeanUtils.copyProperties(paramDto, param);
        if (StringUtils.isNotBlank(paramDto.getTimeStart())) {
            param.setTimeStart(DateUtils.convertToTimeStampFromDateTime(paramDto.getTimeStart()));
        }
        if (StringUtils.isNotBlank(paramDto.getTimeEnd())) {
            param.setTimeEnd(DateUtils.convertToTimeStampFromDateTime(paramDto.getTimeEnd()));
        }
        return param;
    }
}
