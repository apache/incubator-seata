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
package org.apache.seata.common.monitor;

import java.time.Instant;
import java.util.Objects;

public class SqlExecutionEntry {

    private final String sql;
    private final long executionTimeMillis;
    private final long holdTimeMillis;
    private final Instant timestamp;

    public SqlExecutionEntry(String sql, long executionTimeMillis, long holdTimeMillis, Instant timestamp) {
        this.sql = sql;
        this.executionTimeMillis = executionTimeMillis;
        this.holdTimeMillis = holdTimeMillis;
        this.timestamp = timestamp;
    }

    public String getSql() {
        return sql;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }
    public long getHoldTimeMillis() {
        return holdTimeMillis;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SlowSqlEntry{" +
                "sql='" + sql + '\'' +
                ", executionTimeMillis=" + executionTimeMillis +
                ", holdTimeMillis=" + holdTimeMillis +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlExecutionEntry entry = (SqlExecutionEntry) o;
        return executionTimeMillis == entry.executionTimeMillis && holdTimeMillis == entry.holdTimeMillis && Objects.equals(sql, entry.sql) && Objects.equals(timestamp, entry.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, executionTimeMillis, holdTimeMillis, timestamp);
    }
}
