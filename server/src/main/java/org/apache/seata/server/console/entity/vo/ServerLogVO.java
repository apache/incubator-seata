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
package org.apache.seata.server.console.entity.vo;

import java.util.List;

public class ServerLogVO {

    private Integer cursor;

    private List<String> logMessages;

    private Integer curLogNum;

    private Long totalLines;

    public ServerLogVO(Integer cursor, List<String> logMessages, Long totalLines) {
        this.cursor = cursor;
        this.logMessages = logMessages;
        this.totalLines = totalLines;
    }

    public ServerLogVO(Integer cursor, List<String> logMessages, Integer curLogNum, Long totalLines) {
        this.cursor = cursor;
        this.logMessages = logMessages;
        this.curLogNum = curLogNum;
        this.totalLines = totalLines;
    }

    @Override
    public String toString() {
        return "ServerLogVO{" + "cursor="
                + cursor + ", logMessages="
                + logMessages + ", curLogNum="
                + curLogNum + ", totalLines="
                + totalLines + '}';
    }

    public Integer getCursor() {
        return cursor;
    }

    public void setCursor(Integer cursor) {
        this.cursor = cursor;
    }

    public List<String> getLogMessages() {
        return logMessages;
    }

    public void setLogMessages(List<String> logMessages) {
        this.logMessages = logMessages;
    }

    public Integer getCurLogNum() {
        return curLogNum;
    }

    public void setCurLogNum(Integer curLogNum) {
        this.curLogNum = curLogNum;
    }

    public Long getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(Long totalLines) {
        this.totalLines = totalLines;
    }
}
