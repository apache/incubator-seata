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
 * The type Slow sql entry.
 *
 */
public class SlowSqlEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The Sql.
     */
    private String sql;

    /**
     * The Execution time millis.
     */
    private long executionTimeMillis;

    /**
     * The Timestamp.
     */
    private LocalDateTime timestamp;

    public SlowSqlEntry(String sql, long executionTimeMillis, LocalDateTime timestamp) {
        this.sql = sql;
        this.executionTimeMillis = executionTimeMillis;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SlowSqlEntry{" + "sql='"
                + sql + '\'' + ", executionTimeMillis="
                + executionTimeMillis + ", timestamp="
                + timestamp + '}';
    }
}

