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
package org.apache.seata.serializer.seata.protocol;

import io.netty.buffer.ByteBuf;
import org.apache.seata.core.protocol.ConnectionPoolMetricsMessage;
import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.apache.seata.core.protocol.SqlExecutionEntry;
import org.apache.seata.serializer.seata.MessageSeataCodec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seata serializer codec for ConnectionPoolMetricsMessage.
 * Provides compact binary encoding/decoding for metrics message.
 */
public class ConnectionPoolMetricsMessageCodec implements MessageSeataCodec {

    private static final java.nio.charset.Charset UTF8 = StandardCharsets.UTF_8;

    public ConnectionPoolMetricsMessageCodec() {}

    public ConnectionPoolMetricsMessageCodec(byte version) {}

    @Override
    public Class<?> getMessageClassType() {
        return ConnectionPoolMetricsMessage.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        ConnectionPoolMetricsMessage msg = (ConnectionPoolMetricsMessage) t;
        // applicationId
        writeString(out, msg.getApplicationId());
        // clientUrl
        writeString(out, msg.getClientUrl());
        // sequenceNumber, timestamp
        out.writeLong(msg.getSequenceNumber());
        out.writeLong(msg.getTimestamp());

        // Hikari metrics
        List<HikariConnectionPoolMetrics> hList = msg.getHikariMetrics();
        out.writeShort((short) (hList == null ? 0 : hList.size()));
        if (hList != null) {
            for (HikariConnectionPoolMetrics m : hList) {
                writeString(out, m.getPoolName());
                out.writeInt(m.getActiveConnections());
                out.writeInt(m.getIdleConnections());
                out.writeInt(m.getTotalConnections());
                out.writeInt(m.getMaxPoolSize());
                out.writeInt(m.getMinIdle());
                out.writeInt(m.getWaitThreadCount());
                out.writeInt(m.getConnectionTimeout());
                out.writeLong(m.getValidationTimeout());
                out.writeBoolean(m.isAutoCommit());
                out.writeLong(m.getIdleTimeout());
                writeString(out, m.getJdbcUrl());
                writeString(out, m.getDataSourceClassName());
                out.writeLong(m.getTimestamp());
            }
        }

        // Druid metrics
        List<DruidConnectionPoolMetrics> dList = msg.getDruidMetrics();
        out.writeShort((short) (dList == null ? 0 : dList.size()));
        if (dList != null) {
            for (DruidConnectionPoolMetrics m : dList) {
                writeString(out, m.getPoolName());
                out.writeInt(m.getActiveConnections());
                out.writeInt(m.getIdleConnections());
                out.writeInt(m.getTotalConnections());
                out.writeInt(m.getMaxPoolSize());
                out.writeInt(m.getMinIdle());
                out.writeInt(m.getWaitThreadCount());
                out.writeLong(m.getMaxWaitTime());
                out.writeLong(m.getTimeBetweenEvictionRunsMills());
                out.writeLong(m.getMaxEvictableTimeMills());
                out.writeLong(m.getExecuteCount());
                out.writeLong(m.getErrorCount());
                out.writeLong(m.getCommitCount());
                out.writeLong(m.getRollbackCount());
                out.writeLong(m.getLogicConnectCount());
                writeString(out, m.getJdbcUrl());
                out.writeLong(m.getTimestamp());
                // histogram arrays
                long[] ranges = m.getTransactionHistogramRanges();
                long[] values = m.getTransactionHistogramValues();
                out.writeShort((short) (ranges == null ? 0 : ranges.length));
                if (ranges != null) {
                    for (long r : ranges) out.writeLong(r);
                }
                out.writeShort((short) (values == null ? 0 : values.length));
                if (values != null) {
                    for (long v : values) out.writeLong(v);
                }
                // slowSqlList
                List<SqlExecutionEntry> slowList = m.getSlowSqlList();
                out.writeShort((short) (slowList == null ? 0 : slowList.size()));
                if (slowList != null) {
                    for (SqlExecutionEntry e : slowList) {
                        writeString(out, e.getSql());
                        out.writeLong(e.getExecutionTimeMillis());
                        out.writeLong(e.getHoldTimeMillis());
                        writeString(
                                out,
                                e.getTimestamp() == null ? "" : e.getTimestamp().toString());
                    }
                }
                // sqlExecutionRecord
                List<SqlExecutionEntry> execList = m.getSqlExecutionRecord();
                out.writeShort((short) (execList == null ? 0 : execList.size()));
                if (execList != null) {
                    for (SqlExecutionEntry e : execList) {
                        writeString(out, e.getSql());
                        out.writeLong(e.getExecutionTimeMillis());
                        out.writeLong(e.getHoldTimeMillis());
                        writeString(
                                out,
                                e.getTimestamp() == null ? "" : e.getTimestamp().toString());
                    }
                }
            }
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        ConnectionPoolMetricsMessage msg = (ConnectionPoolMetricsMessage) t;
        msg.setApplicationId(readString(in));
        msg.setClientUrl(readString(in));
        msg.setSequenceNumber(in.getLong());
        msg.setTimestamp(in.getLong());

        // Hikari
        int hCount = in.getShort();
        List<HikariConnectionPoolMetrics> hList = new ArrayList<>(hCount);
        for (int i = 0; i < hCount; i++) {
            HikariConnectionPoolMetrics m = new HikariConnectionPoolMetrics(readString(in));
            m.setActiveConnections(in.getInt());
            m.setIdleConnections(in.getInt());
            m.setTotalConnections(in.getInt());
            m.setMaxPoolSize(in.getInt());
            m.setMinIdle(in.getInt());
            m.setWaitThreadCount(in.getInt());
            m.setConnectionTimeout(in.getInt());
            m.setValidationTimeout(in.getLong());
            m.setAutoCommit(in.get() != 0);
            m.setIdleTimeout(in.getLong());
            m.setJdbcUrl(readString(in));
            m.setDataSourceClassName(readString(in));
            m.setTimestamp(in.getLong());
            hList.add(m);
        }
        msg.setHikariMetrics(hList);

        // Druid
        int dCount = in.getShort();
        List<DruidConnectionPoolMetrics> dList = new ArrayList<>(dCount);
        for (int i = 0; i < dCount; i++) {
            DruidConnectionPoolMetrics m = new DruidConnectionPoolMetrics(readString(in));
            m.setActiveConnections(in.getInt());
            m.setIdleConnections(in.getInt());
            m.setTotalConnections(in.getInt());
            m.setMaxPoolSize(in.getInt());
            m.setMinIdle(in.getInt());
            m.setWaitThreadCount(in.getInt());
            m.setMaxWaitTime(in.getLong());
            m.setTimeBetweenEvictionRunsMills(in.getLong());
            m.setMaxEvictableTimeMills(in.getLong());
            m.setExecuteCount(in.getLong());
            m.setErrorCount(in.getLong());
            m.setCommitCount(in.getLong());
            m.setRollbackCount(in.getLong());
            m.setLogicConnectCount(in.getLong());
            m.setJdbcUrl(readString(in));
            m.setTimestamp(in.getLong());
            // histogram arrays
            int rLen = in.getShort();
            if (rLen > 0) {
                long[] ranges = new long[rLen];
                for (int j = 0; j < rLen; j++) ranges[j] = in.getLong();
                m.setTransactionHistogramRanges(ranges);
            }
            int vLen = in.getShort();
            if (vLen > 0) {
                long[] values = new long[vLen];
                for (int j = 0; j < vLen; j++) values[j] = in.getLong();
                m.setTransactionHistogramValues(values);
            }
            // slowSqlList
            int slowCount = in.getShort();
            List<SqlExecutionEntry> slowList = new ArrayList<>(slowCount);
            for (int j = 0; j < slowCount; j++) {
                String sql = readString(in);
                long exec = in.getLong();
                long hold = in.getLong();
                String tsStr = readString(in);
                LocalDateTime ts = tsStr == null || tsStr.isEmpty() ? null : LocalDateTime.parse(tsStr);
                slowList.add(new SqlExecutionEntry(sql, exec, hold, ts));
            }
            m.setSlowSqlList(slowList);
            // sqlExecutionRecord
            int execCount = in.getShort();
            List<SqlExecutionEntry> execList = new ArrayList<>(execCount);
            for (int j = 0; j < execCount; j++) {
                String sql = readString(in);
                long exec = in.getLong();
                long hold = in.getLong();
                String tsStr = readString(in);
                LocalDateTime ts = tsStr == null || tsStr.isEmpty() ? null : LocalDateTime.parse(tsStr);
                execList.add(new SqlExecutionEntry(sql, exec, hold, ts));
            }
            m.setSqlExecutionRecord(execList);
            dList.add(m);
        }
        msg.setDruidMetrics(dList);
    }

    private void writeString(ByteBuf out, String s) {
        if (s == null) {
            out.writeInt(0);
            return;
        }
        byte[] bytes = s.getBytes(UTF8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    private String readString(ByteBuffer in) {
        int len = in.getInt();
        if (len <= 0) {
            return "";
        }
        byte[] bytes = new byte[len];
        in.get(bytes);
        return new String(bytes, UTF8);
    }
}
