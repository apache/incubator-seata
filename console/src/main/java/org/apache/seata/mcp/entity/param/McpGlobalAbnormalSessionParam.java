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

import org.springaicommunity.mcp.annotation.McpToolParam;

public class McpGlobalAbnormalSessionParam {
    @McpToolParam(
            description = "Whether or not it contains branch transaction information, default is true",
            required = false)
    private boolean withBranch = true;

    @McpToolParam(description = "The transaction start time is after this time (yyyy-MM-dd HH:mm:ss)", required = false)
    private String timeStart;

    @McpToolParam(
            description = "The transaction start time is before this time (yyyy-MM-dd HH:mm:ss)",
            required = false)
    private String timeEnd;

    @McpToolParam(description = "Page number")
    private int pageNum;

    public boolean isWithBranch() {
        return withBranch;
    }

    public void setWithBranch(boolean withBranch) {
        this.withBranch = withBranch;
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

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }
}
