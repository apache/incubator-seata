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
import {PoolConfig, PoolMetrics, PoolType} from '../types';

// Generate a random integer within [min, max]
const randomInt = (min: number, max: number) => {
    return Math.floor(Math.random() * (max - min + 1) + min);
};

// Generate a random timestamp within the past 24 hours
const randomTimestamp = () => {
    const now = Date.now();
    return now - randomInt(0, 24 * 60 * 60 * 1000);
};

// Sample SQL statements for mock data
const sampleSQLs = [
    'SELECT * FROM users WHERE id = ?',
    'UPDATE orders SET status = ? WHERE order_id = ?',
    'INSERT INTO products (name, price, category) VALUES (?, ?, ?)',
    'DELETE FROM cart_items WHERE user_id = ? AND expired = true',
    'SELECT o.id, o.date, u.name FROM orders o JOIN users u ON o.user_id = u.id WHERE o.status = ?',
];

// Generate mock metrics for Druid connection pool
const generateDruidMetrics = (serviceName: string): PoolMetrics => {
    const activeCount = randomInt(5, 30);
    const idleCount = randomInt(2, 15);
    const waitCount = randomInt(0, 5);
    const executeCount = randomInt(100, 5000);

    // Generate SQL execution records
    const sqlExecutionRecord = Array.from({length: 20}, () => {
        const executionTimeMillis = randomInt(10, 500);
        return {
            sql: sampleSQLs[randomInt(0, sampleSQLs.length - 1)],
            executionTimeMillis,
            holdTimeMillis: executionTimeMillis + randomInt(5, 100),
            timestamp: new Date(randomTimestamp()).toISOString(),
        };
    });

    // Generate slow SQL list
    const slowSqlList = sqlExecutionRecord
        .filter(record => record.executionTimeMillis > 200)
        .map(record => ({
            sql: record.sql,
            executionTimeMillis: record.executionTimeMillis,
            timestamp: record.timestamp,
        }));

    // Generate transaction duration histogram
    const transactionHistogramRanges = [
        '0-10ms', '10-50ms', '50-100ms', '100-500ms', '500ms-1s', '1s+'
    ];
    const transactionHistogramValues = transactionHistogramRanges.map(() => randomInt(0, 100));

    return {
        serviceName,
        poolType: 'Druid',
        activeConnections: activeCount,
        idleConnections: idleCount,
        totalConnections: activeCount + idleCount,
        maxPoolSize: 50,
        minIdle: 5,
        waitThreadCount: waitCount,
        connectionTimeout: 30000,
        validationTimeout: 5000,
        autoCommit: true,
        idleTimeout: 600000,
        executeCount,
        errorCount: randomInt(0, 50),
        commitCount: randomInt(500, 2000),
        rollbackCount: randomInt(0, 100),
        logicConnectCount: randomInt(1000, 5000),
        slowSqlList,
        sqlExecutionRecord,
        transactionHistogramValues,
        transactionHistogramRanges,
        connectionAcquiredNanos: 0,
        connectionTimeoutRate: 0,
        leakDetectionThreshold: 0,
        poolName: '',
        timestamp: Date.now(),
        lastUpdateTime: new Date().toISOString(),
    };
};

// Generate mock metrics for HikariCP connection pool
const generateHikariCPMetrics = (serviceName: string): PoolMetrics => {
    const activeCount = randomInt(3, 25);
    const idleCount = randomInt(1, 10);
    return {
        serviceName,
        poolType: 'HikariCP',
        activeConnections: activeCount,
        idleConnections: idleCount,
        totalConnections: activeCount + idleCount,
        maxPoolSize: 40,
        minIdle: 5,
        waitThreadCount: randomInt(0, 3),
        connectionTimeout: 30000,
        validationTimeout: 5000,
        autoCommit: true,
        idleTimeout: 600000,
        executeCount: randomInt(80, 4000),
        errorCount: randomInt(0, 20),
        commitCount: randomInt(200, 1000),
        rollbackCount: randomInt(0, 50),
        logicConnectCount: randomInt(500, 2000),
        slowSqlList: [],
        sqlExecutionRecord: [],
        transactionHistogramValues: [],
        transactionHistogramRanges: [],
        connectionAcquiredNanos: randomInt(500000, 50000000), // 0.5ms - 50ms
        connectionTimeoutRate: Math.random() * 0.1, // 0-10%
        leakDetectionThreshold: randomInt(0, 1) ? 0 : randomInt(5000, 30000), // 5s - 30s, sometimes 0 means disabled
        poolName: `${serviceName}-pool`,
        timestamp: Date.now(),
        lastUpdateTime: new Date().toISOString(),
    };
};

// Generate mock configuration for Druid connection pool
const generateDruidConfig = (serviceName: string): PoolConfig => {
    return {
        serviceName,
        poolType: 'Druid',
        maxPoolSize: randomInt(20, 50),
        minIdle: randomInt(5, 15),
        connectionTimeout: randomInt(5000, 30000),
        timeBetweenEvictionRunsMills: randomInt(30000, 60000),
        maxEvictableTimeMills: randomInt(180000, 300000),
        lastUpdateTime: new Date().toISOString(),
    };
};

// Generate mock configuration for HikariCP connection pool
const generateHikariCPConfig = (serviceName: string): PoolConfig => {
    return {
        serviceName,
        poolType: 'HikariCP',
        maxPoolSize: randomInt(20, 40),
        minIdle: randomInt(5, 10),
        connectionTimeout: randomInt(5000, 30000),
        maxLifeTime: randomInt(1200000, 1800000),
        keepaliveTime: randomInt(10000, 60000),
        lastUpdateTime: new Date().toISOString(),
    };
};

// Service name list
const serviceNames = [
    'user-service',
    'order-service',
    'product-service',
    'payment-service',
    'inventory-service',
    'notification-service',
    'auth-service',
    'search-service',
];
// Generate mock data set
export const generateMockData = () => {
    const metrics: PoolMetrics[] = [];
    const configs: PoolConfig[] = [];

    serviceNames.forEach(serviceName => {
        // Randomly choose connection pool type
        const poolType: PoolType = Math.random() > 0.5 ? 'Druid' : 'HikariCP';

        if (poolType === 'Druid') {
            metrics.push(generateDruidMetrics(serviceName));
            configs.push(generateDruidConfig(serviceName));
        } else {
            metrics.push(generateHikariCPMetrics(serviceName));
            configs.push(generateHikariCPConfig(serviceName));
        }
    });
    return {metrics, configs};
};
// Mocked API responses
export const mockPoolService = {
    getAllPoolMetrics: async (): Promise<PoolMetrics[]> => {
        return new Promise(resolve => {
            setTimeout(() => {
                resolve(generateMockData().metrics);
            }, 500);
        });
    },
    getPoolMetricsByService: async (serviceName: string): Promise<PoolMetrics> => {
        return new Promise(resolve => {
            setTimeout(() => {
                const {metrics} = generateMockData();
                const metric = metrics.find(m => m.serviceName === serviceName);
                resolve(metric || metrics[0]);
            }, 300);
        });
    },
    getPoolMetricsByType: async (poolType: PoolType): Promise<PoolMetrics[]> => {
        return new Promise(resolve => {
            setTimeout(() => {
                const {metrics} = generateMockData();
                const filteredMetrics = poolType === 'All'
                    ? metrics
                    : metrics.filter(m => m.poolType === poolType);
                resolve(filteredMetrics);
            }, 300);
        });
    },
    getAllPoolConfigs: async (): Promise<PoolConfig[]> => {
        return new Promise(resolve => {
            setTimeout(() => {
                resolve(generateMockData().configs);
            }, 500);
        });
    },
    getPoolConfigByService: async (serviceName: string): Promise<PoolConfig> => {
        return new Promise(resolve => {
            setTimeout(() => {
                const {configs} = generateMockData();
                const config = configs.find(c => c.serviceName === serviceName);
                resolve(config || configs[0]);
            }, 300);
        });
    },
    getPoolConfigByType: async (poolType: PoolType): Promise<PoolConfig[]> => {
        return new Promise(resolve => {
            setTimeout(() => {
                const {configs} = generateMockData();
                const filteredConfigs = poolType === 'All'
                    ? configs
                    : configs.filter(c => c.poolType === poolType);
                resolve(filteredConfigs);
            }, 300);
        });
    },
    updatePoolConfig: async (serviceName: string, config: any): Promise<boolean> => {
        return new Promise(resolve => {
            setTimeout(() => {
                console.log(`模拟更新服务 ${serviceName} 的配置:`, config);
                resolve(true);
            }, 1000);
        });
    },
};