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

import org.apache.seata.mcp.annotation.ToolParam;

/**
 * undo_log Query parameters
 */
public class UndoLogParam {

    @ToolParam(description = "Unique identifier of the data source", required = true)
    private String resourceId;

    @ToolParam(description = "Branch transaction ID")
    private String branchId;

    @ToolParam(description = "Global transaction ID")
    private String xid;

    @ToolParam(description = "status,0:normal status,1:defense status")
    private Integer logStatus;

    @ToolParam(description = "The time period created, start time and end time can be the same")
    private CreateTime logCreateTime;

    @ToolParam(description = "The time period modified, start time and end time can be the same")
    private ModifyTime logModifiedTime;

    @ToolParam(description = "PageNum", required = true, example = "1")
    private int pageNum;

    @ToolParam(description = "PageSize", required = true, example = "100")
    private int pageSize;

    public static class CreateTime {
        @ToolParam(description = "Start time", example = "2025-07-09 11:38:45, yyyy-MM-dd HH:mm:ss")
        private String startTime;

        @ToolParam(description = "End time", example = "2025-07-09 11:38:45, yyyy-MM-dd HH:mm:ss")
        private String endTime;

        @Override
        public String toString() {
            return "CreateTime{" + "startTime=" + startTime + ", endTime=" + endTime + '}';
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }

    public static class ModifyTime {
        @ToolParam(description = "Start time", example = "2025-07-09 11:38:45")
        private String startTime;

        @ToolParam(description = "End time", example = "2025-07-09 11:38:45")
        private String endTime;

        @Override
        public String toString() {
            return "ModifyTime{" + "startTime=" + startTime + ", endTime=" + endTime + '}';
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
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

    public Integer getLogStatus() {
        return logStatus;
    }

    public void setLogStatus(Integer logStatus) {
        this.logStatus = logStatus;
    }

    public CreateTime getLogCreateTime() {
        return logCreateTime;
    }

    public void setLogCreateTime(CreateTime logCreateTime) {
        this.logCreateTime = logCreateTime;
    }

    public ModifyTime getLogModifiedTime() {
        return logModifiedTime;
    }

    public void setLogModifiedTime(ModifyTime logModifiedTime) {
        this.logModifiedTime = logModifiedTime;
    }

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
}
