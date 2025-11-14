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
package org.apache.seata.core.protocol;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The Sql Execution Entry
 *
 */
public class SqlExecutionEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The sql.
     */
    private String sql;

    /**
     * The execution time millis.
     */
    private long executionTimeMillis;

    /**
     * The hold time millis.
     */
    private long holdTimeMillis;

    /**
     * The timestamp.
     */
    private LocalDateTime timestamp;

    public SqlExecutionEntry(String sql, long executionTimeMillis, long holdTimeMillis, LocalDateTime timestamp) {
        this.sql = sql;
        this.executionTimeMillis = executionTimeMillis;
        this.holdTimeMillis = holdTimeMillis;
        this.timestamp = timestamp;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    public void setExecutionTimeMillis(long executionTimeMillis) {
        this.executionTimeMillis = executionTimeMillis;
    }

    public long getHoldTimeMillis() {
        return holdTimeMillis;
    }

    public void setHoldTimeMillis(long holdTimeMillis) {
        this.holdTimeMillis = holdTimeMillis;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SqlExecutionEntry{" + "sql='"
                + sql + '\'' + ", executionTimeMillis="
                + executionTimeMillis + ", holdTimeMillis="
                + holdTimeMillis + ", timestamp="
                + timestamp + '}';
    }
}
