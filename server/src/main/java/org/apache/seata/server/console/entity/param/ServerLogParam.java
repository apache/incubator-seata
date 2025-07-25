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
package org.apache.seata.server.console.entity.param;

import java.io.Serializable;

public class ServerLogParam implements Serializable {

    private static final long serialVersionUID = 225478653801012285L;

    private Integer cursor;

    private Integer nextLines;

    private String logType;

    private String logTime;

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
