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
package org.apache.seata.serializer.protobuf.convertor;

import org.apache.seata.core.protocol.ConnectionPoolMetricsMessage;
import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.apache.seata.core.protocol.SqlExecutionEntry;
import org.apache.seata.serializer.protobuf.generated.ConnectionPoolMetricsMessageProto;
import org.apache.seata.serializer.protobuf.generated.DruidConnectionPoolMetricsProto;
import org.apache.seata.serializer.protobuf.generated.HikariConnectionPoolMetricsProto;
import org.apache.seata.serializer.protobuf.generated.SqlExecutionEntryProto;

import java.util.ArrayList;
import java.util.List;

/**
 * Connection pool metrics message converter for mutual conversion between ConnectionPoolMetricsMessage and ConnectionPoolMetricsMessageProto.
 * Implements the PbConvertor interface to support Protocol Buffer serialization and deserialization.
 *
 */
public class ConnectionPoolMetricsMessageConvertor
        implements PbConvertor<ConnectionPoolMetricsMessage, ConnectionPoolMetricsMessageProto> {

    @Override
    public ConnectionPoolMetricsMessageProto convert2Proto(ConnectionPoolMetricsMessage msg) {
        ConnectionPoolMetricsMessageProto.Builder builder = ConnectionPoolMetricsMessageProto.newBuilder();
        builder.setApplicationId(msg.getApplicationId() == null ? "" : msg.getApplicationId());
        builder.setClientUrl(msg.getClientUrl() == null ? "" : msg.getClientUrl());
        builder.setSequenceNumber(msg.getSequenceNumber());
        builder.setTimestamp(msg.getTimestamp());
        if (msg.getHikariMetrics() != null) {
            for (HikariConnectionPoolMetrics m : msg.getHikariMetrics()) {
                builder.addHikariMetrics(convertHikari2Proto(m));
            }
        }
        if (msg.getDruidMetrics() != null) {
            for (DruidConnectionPoolMetrics m : msg.getDruidMetrics()) {
                builder.addDruidMetrics(convertDruid2Proto(m));
            }
        }
        return builder.build();
    }

    @Override
    public ConnectionPoolMetricsMessage convert2Model(ConnectionPoolMetricsMessageProto proto) {
        ConnectionPoolMetricsMessage msg = new ConnectionPoolMetricsMessage();
        msg.setApplicationId(proto.getApplicationId());
        msg.setClientUrl(proto.getClientUrl());
        msg.setSequenceNumber(proto.getSequenceNumber());
        msg.setTimestamp(proto.getTimestamp());
        java.util.List<HikariConnectionPoolMetrics> hikariList = new ArrayList<>();
        for (HikariConnectionPoolMetricsProto p : proto.getHikariMetricsList()) {
            hikariList.add(convertHikari2Model(p));
        }
        msg.setHikariMetrics(hikariList);
        List<DruidConnectionPoolMetrics> druidList = new ArrayList<>();
        for (DruidConnectionPoolMetricsProto p : proto.getDruidMetricsList()) {
            druidList.add(convertDruid2Model(p));
        }
        msg.setDruidMetrics(druidList);
        return msg;
    }

    private HikariConnectionPoolMetricsProto convertHikari2Proto(HikariConnectionPoolMetrics m) {
        HikariConnectionPoolMetricsProto.Builder b = HikariConnectionPoolMetricsProto.newBuilder();
        b.setPoolName(nullToEmpty(m.getPoolName()));
        b.setActiveConnections(m.getActiveConnections());
        b.setIdleConnections(m.getIdleConnections());
        b.setTotalConnections(m.getTotalConnections());
        b.setMaxPoolSize(m.getMaxPoolSize());
        b.setMinIdle(m.getMinIdle());
        b.setWaitThreadCount(m.getWaitThreadCount());
        b.setConnectionTimeout(m.getConnectionTimeout());
        b.setValidationTimeout(m.getValidationTimeout());
        b.setAutoCommit(m.isAutoCommit());
        b.setIdleTimeout(m.getIdleTimeout());
        b.setJdbcUrl(nullToEmpty(m.getJdbcUrl()));
        b.setDataSourceClassName(nullToEmpty(m.getDataSourceClassName()));
        b.setTimestamp(m.getTimestamp());
        return b.build();
    }

    private HikariConnectionPoolMetrics convertHikari2Model(HikariConnectionPoolMetricsProto p) {
        HikariConnectionPoolMetrics m = new HikariConnectionPoolMetrics(p.getPoolName());
        m.setActiveConnections(p.getActiveConnections());
        m.setIdleConnections(p.getIdleConnections());
        m.setTotalConnections(p.getTotalConnections());
        m.setMaxPoolSize(p.getMaxPoolSize());
        m.setMinIdle(p.getMinIdle());
        m.setWaitThreadCount(p.getWaitThreadCount());
        m.setConnectionTimeout(p.getConnectionTimeout());
        m.setValidationTimeout(p.getValidationTimeout());
        m.setAutoCommit(p.getAutoCommit());
        m.setIdleTimeout(p.getIdleTimeout());
        m.setJdbcUrl(p.getJdbcUrl());
        m.setDataSourceClassName(p.getDataSourceClassName());
        m.setTimestamp(p.getTimestamp());
        return m;
    }

    private DruidConnectionPoolMetricsProto convertDruid2Proto(DruidConnectionPoolMetrics m) {
        DruidConnectionPoolMetricsProto.Builder b = DruidConnectionPoolMetricsProto.newBuilder();
        b.setPoolName(nullToEmpty(m.getPoolName()));
        b.setActiveConnections(m.getActiveConnections());
        b.setIdleConnections(m.getIdleConnections());
        b.setTotalConnections(m.getTotalConnections());
        b.setMaxPoolSize(m.getMaxPoolSize());
        b.setMinIdle(m.getMinIdle());
        b.setWaitThreadCount(m.getWaitThreadCount());
        b.setMaxWaitTime(m.getMaxWaitTime());
        b.setTimeBetweenEvictionRunsMills(m.getTimeBetweenEvictionRunsMills());
        b.setMaxEvictableTimeMills(m.getMaxEvictableTimeMills());
        b.setExecuteCount(m.getExecuteCount());
        b.setErrorCount(m.getErrorCount());
        b.setCommitCount(m.getCommitCount());
        b.setRollbackCount(m.getRollbackCount());
        b.setLogicConnectCount(m.getLogicConnectCount());
        b.setJdbcUrl(nullToEmpty(m.getJdbcUrl()));
        b.setTimestamp(m.getTimestamp());
        if (m.getSlowSqlList() != null) {
            for (SqlExecutionEntry e : m.getSlowSqlList()) {
                b.addSlowSqlList(convertSqlExec2Proto(e));
            }
        }
        if (m.getSqlExecutionRecord() != null) {
            for (SqlExecutionEntry e : m.getSqlExecutionRecord()) {
                b.addSqlExecutionRecord(convertSqlExec2Proto(e));
            }
        }
        if (m.getTransactionHistogramRanges() != null) {
            b.addAllTransactionHistogramRanges(toLongList(m.getTransactionHistogramRanges()));
        }
        if (m.getTransactionHistogramValues() != null) {
            b.addAllTransactionHistogramValues(toLongList(m.getTransactionHistogramValues()));
        }
        return b.build();
    }

    private DruidConnectionPoolMetrics convertDruid2Model(DruidConnectionPoolMetricsProto p) {
        DruidConnectionPoolMetrics m = new DruidConnectionPoolMetrics(p.getPoolName());
        m.setActiveConnections(p.getActiveConnections());
        m.setIdleConnections(p.getIdleConnections());
        m.setTotalConnections(p.getTotalConnections());
        m.setMaxPoolSize(p.getMaxPoolSize());
        m.setMinIdle(p.getMinIdle());
        m.setWaitThreadCount(p.getWaitThreadCount());
        m.setMaxWaitTime(p.getMaxWaitTime());
        m.setTimeBetweenEvictionRunsMills(p.getTimeBetweenEvictionRunsMills());
        m.setMaxEvictableTimeMills(p.getMaxEvictableTimeMills());
        m.setExecuteCount(p.getExecuteCount());
        m.setErrorCount(p.getErrorCount());
        m.setCommitCount(p.getCommitCount());
        m.setRollbackCount(p.getRollbackCount());
        m.setLogicConnectCount(p.getLogicConnectCount());
        m.setJdbcUrl(p.getJdbcUrl());
        m.setTimestamp(p.getTimestamp());
        List<SqlExecutionEntry> slowList = new ArrayList<>();
        for (SqlExecutionEntryProto ep : p.getSlowSqlListList()) {
            slowList.add(convertSqlExec2Model(ep));
        }
        m.setSlowSqlList(slowList);
        List<SqlExecutionEntry> execList = new ArrayList<>();
        for (SqlExecutionEntryProto ep : p.getSqlExecutionRecordList()) {
            execList.add(convertSqlExec2Model(ep));
        }
        m.setSqlExecutionRecord(execList);
        if (p.getTransactionHistogramRangesList() != null
                && !p.getTransactionHistogramRangesList().isEmpty()) {
            m.setTransactionHistogramRanges(toLongArray(p.getTransactionHistogramRangesList()));
        }
        if (p.getTransactionHistogramValuesList() != null
                && !p.getTransactionHistogramValuesList().isEmpty()) {
            m.setTransactionHistogramValues(toLongArray(p.getTransactionHistogramValuesList()));
        }
        return m;
    }

    private SqlExecutionEntryProto convertSqlExec2Proto(SqlExecutionEntry e) {
        SqlExecutionEntryProto.Builder b = SqlExecutionEntryProto.newBuilder();
        b.setSql(nullToEmpty(e.getSql()));
        b.setExecutionTimeMillis(e.getExecutionTimeMillis());
        b.setHoldTimeMillis(e.getHoldTimeMillis());
        b.setTimestamp(e.getTimestamp() == null ? "" : e.getTimestamp().toString());
        return b.build();
    }

    private SqlExecutionEntry convertSqlExec2Model(SqlExecutionEntryProto ep) {
        java.time.LocalDateTime ts = null;
        try {
            String tsStr = ep.getTimestamp();
            if (tsStr != null && !tsStr.isEmpty()) {
                ts = java.time.LocalDateTime.parse(tsStr);
            }
        } catch (Exception ignore) {
        }
        return new SqlExecutionEntry(ep.getSql(), ep.getExecutionTimeMillis(), ep.getHoldTimeMillis(), ts);
    }

    private List<Long> toLongList(long[] arr) {
        List<Long> list = new ArrayList<>();
        for (long v : arr) list.add(v);
        return list;
    }

    private long[] toLongArray(List<Long> list) {
        long[] arr = new long[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
