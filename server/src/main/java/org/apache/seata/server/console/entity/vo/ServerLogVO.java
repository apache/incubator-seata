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

    private Long costedTime;

    private List<String> logMessages;

    public ServerLogVO (Integer cursor, Long costedTime, List<String> logMessages){
        this.cursor = cursor;
        this.costedTime = costedTime;
        this.logMessages = logMessages;
    }

    public Integer getCursor() {
        return cursor;
    }

    public void setCursor(Integer cursor) {
        this.cursor = cursor;
    }

    public Long getCostedTime() {
        return costedTime;
    }

    public void setCostedTime(Long costedTime) {
        this.costedTime = costedTime;
    }

    public List<String> getLogs() {
        return logMessages;
    }

    public void setLogs(List<String> logMessages) {
        this.logMessages = logMessages;
    }

    @Override
    public String toString() {
        return "ServerLogVO{" +
                "cursor=" + cursor +
                ", costedTime=" + costedTime +
                ", logs=" + logMessages +
                '}';
    }
}
