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

import java.io.Serializable;

public class ServerLogParam implements Serializable {

    private static final long serialVersionUID = 225478653801012285L;

    @ToolParam(description = "Log type filter, optional values: all, error, warn, default is all", example = "all")
    private String logType = "all";

    @ToolParam(
            description =
                    "Page number index (starting from 1). The larger the page number, the newer the log, and you can directly get the latest log by specifying the maximum page number",
            required = true,
            example = "1")
    private Integer page = 1;

    @ToolParam(description = "The start time when the log information was generated(yyyy-MM-dd HH:mm:ss)")
    private String logMessageStartTime;

    @ToolParam(description = "The end time when the log information was generated(yyyy-MM-dd HH:mm:ss)")
    private String logMessageEndTime;

    @ToolParam(
            description =
                    "Log level filtering, optional values: error, warn, info. When there is a conflict with the logType parameter, the logType shall prevail",
            example = "error")
    private String logMessageLevel;

    @ToolParam(
            description =
                    "This is a Array of String type, without key-value format!! Log content keyword fuzzy matching, support multiple keywords separated by commas, All keyword matches will return the log line information")
    private String[] logMessageKeyWord;

    private Long curSize;

    private Long lastModifyTime;

    public Long getCurSize() {
        return curSize;
    }

    public void setCurSize(Long curSize) {
        this.curSize = curSize;
    }

    public Long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getLogMessageStartTime() {
        return logMessageStartTime;
    }

    public void setLogMessageStartTime(String logMessageStartTime) {
        this.logMessageStartTime = logMessageStartTime;
    }

    public String getLogMessageEndTime() {
        return logMessageEndTime;
    }

    public void setLogMessageEndTime(String logMessageEndTime) {
        this.logMessageEndTime = logMessageEndTime;
    }

    public String getLogMessageLevel() {
        return logMessageLevel;
    }

    public void setLogMessageLevel(String logMessageLevel) {
        this.logMessageLevel = logMessageLevel;
    }

    public String[] getLogMessageKeyWord() {
        return logMessageKeyWord;
    }

    public void setLogMessageKeyWord(String[] logMessageKeyWord) {
        this.logMessageKeyWord = logMessageKeyWord;
    }
}
