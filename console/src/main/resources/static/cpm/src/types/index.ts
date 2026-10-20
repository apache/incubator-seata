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
// Connection pool type
export type PoolType = 'All' | 'Druid' | 'HikariCP';

// Base connection pool metrics
export interface PoolMetrics {
    serviceName: string;
    poolType: PoolType;
    // Common fields (supported by both Druid & HikariCP)
    activeConnections: number;
    idleConnections: number;
    totalConnections: number;
    maxPoolSize: number;
    minIdle: number;
    waitThreadCount: number;
    connectionTimeout: number;
    validationTimeout: number;
    autoCommit: boolean;
    idleTimeout: number;
    // Druid-specific fields
    executeCount: number;
    errorCount: number;
    commitCount: number;
    rollbackCount: number;
    logicConnectCount: number;
    slowSqlList: SlowSql[];
    sqlExecutionRecord: SqlExecution[];
    transactionHistogramValues: number[];
    transactionHistogramRanges: string[];
    // HikariCP-specific fields
    connectionAcquiredNanos: number;
    connectionTimeoutRate: number;
    leakDetectionThreshold: number;
    poolName: string;
    timestamp: number;
    // Common fields
    lastUpdateTime: string;
}

// Slow SQL info
export interface SlowSql {
    sql: string;
    executionTimeMillis: number;
    timestamp: string;
}

// SQL execution record
export interface SqlExecution {
    sql: string;
    executionTimeMillis: number;
    holdTimeMillis: number;
    timestamp: string;
}

// Connection pool configuration
export interface PoolConfig {
    serviceName: string;
    poolType?: PoolType; // Optional because backend may omit it
    // Common fields
    maxPoolSize: number;
    minIdle: number;
    connectionTimeout: number;
    // Druid-specific configuration
    timeBetweenEvictionRunsMills?: number;
    maxEvictableTimeMills?: number;
    // HikariCP-specific configuration
    maxLifeTime?: number;
    keepaliveTime?: number;
    // Common fields
    lastUpdateTime?: string;
}

// Full connection pool info (metrics + configuration)
export interface PoolInfo {
    metrics: PoolMetrics;
    config: PoolConfig;
}