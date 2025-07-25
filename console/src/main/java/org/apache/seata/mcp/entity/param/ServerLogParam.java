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

    @ToolParam(description = "file pointer, i.e. read the log file from the cursor line",required = true)
    private Integer cursor;

    @ToolParam(description = "The number of rows read down from the cursor line should not exceed 100 lines",required = true)
    private Integer nextLines;

    @ToolParam(description = "log type, contains: all, error, warn")
    private String logType;

    @ToolParam(description = "Log creation time, format: yyyy-mm-dd, It is only required to pass in when querying the history log")
    private String logTime;

    @ToolParam(description = "The log serial number to be analyzed, It is only required to pass in when querying the history log")
    private Integer curLogNum;

    public Integer getCurLogNum() {
        return curLogNum;
    }

    public void setCurLogNum(Integer curLogNum) {
        this.curLogNum = curLogNum;
    }

    public Integer getNextLines() {
        return nextLines;
    }

    public void setNextLines(Integer nextLines) {
        this.nextLines = nextLines;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getLogTime() {
        return logTime;
    }

    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    public Integer getCursor() {
        return cursor;
    }

    public void setCursor(Integer cursor) {
        this.cursor = cursor;
    }

    @Override
    public String toString() {
        return "ServerLogParam{" +
                "cursor=" + cursor +
                ", nextLines=" + nextLines +
                ", logType='" + logType + '\'' +
                ", logTime='" + logTime + '\'' +
                ", curLogNum=" + curLogNum +
                '}';
    }
}
