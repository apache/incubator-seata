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
package org.apache.seata.mcp.entity.dto;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.GlobalAbnormalSessionParam;

import java.io.Serializable;

public class GlobalSessionParamDto implements Serializable {

    private static final long serialVersionUID = 115488252809011284L;

    @ToolParam(description = "GLOBAL TRANSACTIONS id")
    private String xid;

    @ToolParam(description = "applicationId")
    private String applicationId;

    @ToolParam(
            description = "the state enumeration class is in example",
            exampleValueClassName = {GlobalStatus.class, BranchStatus.class})
    private Integer status;

    @ToolParam(description = "The name of the transaction")
    private String transactionName;

    @ToolParam(description = "Whether or not it contains branch transaction information")
    private boolean withBranch;

    @ToolParam(description = "PAGE NUMBER", required = true, example = "1")
    private int pageNum;

    @ToolParam(description = "PageSize", required = true, example = "100")
    private int pageSize;

    @ToolParam(description = "The transaction start time is after this time (yyyy-MM-dd HH:mm:ss)")
    private String timeStart;

    @ToolParam(description = "The transaction start time is before this time (yyyy-MM-dd HH:mm:ss)")
    private String timeEnd;

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

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
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

    public static GlobalSessionParamDto covertFromAbnormalParam(GlobalAbnormalSessionParam abParam) {
        GlobalSessionParamDto param = new GlobalSessionParamDto();
        if (StringUtils.isNotBlank(abParam.getTimeStart())) {
            param.setTimeStart(abParam.getTimeStart());
        }
        if (StringUtils.isNotBlank(abParam.getTimeEnd())) {
            param.setTimeEnd(abParam.getTimeEnd());
        }
        param.setWithBranch(abParam.isWithBranch());
        return param;
    }
}
