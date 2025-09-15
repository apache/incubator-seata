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

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.dto.GlobalSessionParamDto;
import org.apache.seata.mcp.utils.DateUtils;

import java.io.Serializable;

/**
 * Global session param
 */
public class GlobalSessionParam implements Serializable {

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
        return "GlobalSessionParam{" + "xid='"
                + xid + '\'' + ", applicationId='"
                + applicationId + '\'' + ", status="
                + status + ", transactionName='"
                + transactionName + '\'' + ", vgroup='"
                + withBranch + ", pageNum="
                + pageNum + ", pageSize="
                + pageSize + ", timeStart="
                + timeStart + ", timeEnd="
                + timeEnd + '}';
    }

    public static GlobalSessionParam covertFromAbnormalParam(GlobalAbnormalSessionParam abParam) {
        GlobalSessionParam param = new GlobalSessionParam();
        if (StringUtils.isNotBlank(abParam.getTimeStart())) {
            param.setTimeStart(DateUtils.convertToTimeStampFromDateTime(abParam.getTimeStart()));
        }
        if (StringUtils.isNotBlank(abParam.getTimeEnd())) {
            param.setTimeEnd(DateUtils.convertToTimeStampFromDateTime(abParam.getTimeEnd()));
        }
        param.setWithBranch(abParam.isWithBranch());
        return param;
    }

    public static GlobalSessionParam covertFromDtoParam(GlobalSessionParamDto paramDto) {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageSize(paramDto.getPageSize());
        param.setPageNum(paramDto.getPageNum());
        param.setStatus(paramDto.getStatus());
        param.setXid(paramDto.getXid());
        param.setApplicationId(paramDto.getApplicationId());
        param.setTransactionName(paramDto.getTransactionName());
        param.setWithBranch(paramDto.isWithBranch());
        if (StringUtils.isNotBlank(paramDto.getTimeStart())) {
            param.setTimeStart(DateUtils.convertToTimeStampFromDateTime(paramDto.getTimeStart()));
        }
        if (StringUtils.isNotBlank(paramDto.getTimeEnd())) {
            param.setTimeEnd(DateUtils.convertToTimeStampFromDateTime(paramDto.getTimeEnd()));
        }
        return param;
    }
}
