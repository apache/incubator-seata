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
import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// 直接定义翻译资源，避免JSON导入问题
const zhCN = {
    "common": {
        "loading": "加载中...",
        "search": "搜索",
        "edit": "编辑",
        "save": "保存",
        "cancel": "取消",
        "confirm": "确认",
        "all": "全部",
        "refresh": "刷新",
        "close": "关闭"
    },
    "header": {
        "title": "连接池监控系统",
        "language": "语言"
    },
    "filter": {
        "title": "连接池监控",
        "poolType": "连接池类型:",
        "searchPlaceholder": "搜索服务名称"
    },
    "table": {
        "serviceName": "服务名称",
        "poolType": "连接池类型",
        "activeConnections": "活跃连接",
        "idleConnections": "空闲连接",
        "totalConnections": "总连接数",
        "maxPoolSize": "最大连接数",
        "status": "状态",
        "actions": "操作",
        "details": "详情",
        "charts": "图表",
        "config": "配置",
        "editConfig": "编辑配置"
    },
    "metrics": {
        "activeConnections": "活跃连接",
        "idleConnections": "空闲连接",
        "totalConnections": "总连接数",
        "maxPoolSize": "最大连接数",
        "waitThreadCount": "等待线程数",
        "connectionTimeout": "连接超时",
        "validationTimeout": "验证超时",
        "autoCommit": "自动提交",
        "idleTimeout": "空闲超时",
        "totalActiveConnections": "总活跃连接数",
        "totalIdleConnections": "总空闲连接数",
        "totalWaitThreadCount": "总等待数",
        "totalExecuteCount": "总执行数"
    },
    "druid": {
        "executeCount": "执行次数",
        "errorCount": "错误次数",
        "commitCount": "提交次数",
        "rollbackCount": "回滚次数",
        "logicConnectCount": "逻辑连接次数",
        "slowSqlList": "慢SQL列表",
        "sqlExecutionRecord": "SQL执行记录",
        "transactionHistogram": "事务直方图",
        "timeBetweenEvictionRuns": "清理间隔时间",
        "maxEvictableTime": "最大清理时间"
    },
    "hikaricp": {
        "connectionAcquiredNanos": "连接获取时间(纳秒)",
        "connectionTimeoutRate": "连接超时率",
        "leakDetectionThreshold": "泄漏检测阈值",
        "poolName": "连接池名称",
        "maxLifeTime": "最大生命周期",
        "keepaliveTime": "保活时间"
    },
    "config": {
        "title": "连接池配置",
        "basicConfig": "基础配置",
        "advancedConfig": "高级配置",
        "druidSpecific": "Druid特有配置",
        "hikariSpecific": "HikariCP特有配置",
        "maxPoolSize": "最大连接数",
        "minIdle": "最小空闲连接",
        "connectionTimeout": "连接超时时间(ms)",
        "validationTimeout": "验证超时时间(ms)",
        "autoCommit": "自动提交",
        "idleTimeout": "空闲超时时间(ms)",
        "maxLifeTime": "最大生命周期(ms)",
        "keepaliveTime": "保活时间(ms)",
        "timeBetweenEvictionRuns": "清理间隔时间(ms)",
        "maxEvictableTime": "最大清理时间(ms)",
        "updateSuccess": "配置更新成功",
        "updateFailed": "配置更新失败"
    },
    "status": {
        "healthy": "健康",
        "warning": "警告",
        "error": "错误"
    },
    "charts": {
        "connectionTrend": "连接数趋势",
        "performanceMetrics": "性能指标",
        "errorRate": "错误率",
        "responseTime": "响应时间",
        "noData": "暂无连接池监控数据",
        "sqlExecutionTime": "SQL执行时间折线图",
        "executionTime": "执行时间",
        "time": "时间",
        "connectionHoldTime": "连接持有时间曲线图",
        "holdTime": "持有时间",
        "transactionDistribution": "事务耗时分布柱状图",
        "transactionCount": "事务数",
        "timeRange": "耗时范围",
        "seconds": "秒",
        "milliseconds": "毫秒",
        "connectionAcquiredTooltip": "获取连接所需的平均时间（毫秒）",
        "connectionTimeoutTooltip": "连接获取超时的比率",
        "highTimeoutRate": "超时率较高",
        "leakDetectionTooltip": "连接泄漏检测阈值，超过此时间未关闭的连接将被视为泄漏",
        "leakDetectionDisabled": "泄漏检测已禁用",
        "leakDetectionEnabled": "泄漏检测已启用",
        "hikariHealthStatus": "HikariCP连接池健康状况",
        "activeConnectionRatio": "活跃连接数/最大连接数比例",
        "waitingConnections": "等待获取连接数",
        "hasWaitingRequests": "存在等待连接的请求",
        "noWaiting": "无等待",
        "totalSqlExecutions": "总执行SQL数"
    }
};

const enUS = {
    "common": {
        "loading": "Loading...",
        "search": "Search",
        "edit": "Edit",
        "save": "Save",
        "cancel": "Cancel",
        "confirm": "Confirm",
        "all": "All",
        "refresh": "Refresh",
        "close": "Close"
    },
    "header": {
        "title": "Connection Pool Monitoring System",
        "language": "Language"
    },
    "filter": {
        "title": "Connection Pool Monitor",
        "poolType": "Pool Type:",
        "searchPlaceholder": "Search service name"
    },
    "table": {
        "serviceName": "Service Name",
        "poolType": "Pool Type",
        "activeConnections": "Active Connections",
        "idleConnections": "Idle Connections",
        "totalConnections": "Total Connections",
        "maxPoolSize": "Max Pool Size",
        "status": "Status",
        "actions": "Actions",
        "details": "Details",
        "charts": "Charts",
        "config": "Config",
        "editConfig": "Edit Config"
    },
    "metrics": {
        "activeConnections": "Active Connections",
        "idleConnections": "Idle Connections",
        "totalConnections": "Total Connections",
        "maxPoolSize": "Max Pool Size",
        "waitThreadCount": "Wait Thread Count",
        "connectionTimeout": "Connection Timeout",
        "validationTimeout": "Validation Timeout",
        "autoCommit": "Auto Commit",
        "idleTimeout": "Idle Timeout",
        "totalActiveConnections": "Total Active Connections",
        "totalIdleConnections": "Total Idle Connections",
        "totalWaitThreadCount": "Total Wait Thread Count",
        "totalExecuteCount": "Total Execute Count"
    },
    "druid": {
        "executeCount": "Execute Count",
        "errorCount": "Error Count",
        "commitCount": "Commit Count",
        "rollbackCount": "Rollback Count",
        "logicConnectCount": "Logic Connect Count",
        "slowSqlList": "Slow SQL List",
        "sqlExecutionRecord": "SQL Execution Record",
        "transactionHistogram": "Transaction Histogram",
        "timeBetweenEvictionRuns": "Time Between Eviction Runs",
        "maxEvictableTime": "Max Evictable Time"
    },
    "hikaricp": {
        "connectionAcquiredNanos": "Connection Acquired Nanos",
        "connectionTimeoutRate": "Connection Timeout Rate",
        "leakDetectionThreshold": "Leak Detection Threshold",
        "poolName": "Pool Name",
        "maxLifeTime": "Max Life Time",
        "keepaliveTime": "Keepalive Time"
    },
    "config": {
        "title": "Connection Pool Configuration",
        "basicConfig": "Basic Configuration",
        "advancedConfig": "Advanced Configuration",
        "druidSpecific": "Druid Specific Configuration",
        "hikariSpecific": "HikariCP Specific Configuration",
        "maxPoolSize": "Max Pool Size",
        "minIdle": "Min Idle Connections",
        "connectionTimeout": "Connection Timeout (ms)",
        "validationTimeout": "Validation Timeout (ms)",
        "autoCommit": "Auto Commit",
        "idleTimeout": "Idle Timeout (ms)",
        "maxLifeTime": "Max Life Time (ms)",
        "keepaliveTime": "Keepalive Time (ms)",
        "timeBetweenEvictionRuns": "Time Between Eviction Runs (ms)",
        "maxEvictableTime": "Max Evictable Time (ms)",
        "updateSuccess": "Configuration updated successfully",
        "updateFailed": "Failed to update configuration"
    },
    "status": {
        "healthy": "Healthy",
        "warning": "Warning",
        "error": "Error"
    },
    "charts": {
        "connectionTrend": "Connection Trend",
        "performanceMetrics": "Performance Metrics",
        "errorRate": "Error Rate",
        "responseTime": "Response Time",
        "noData": "No connection pool monitoring data available",
        "sqlExecutionTime": "SQL Execution Time Chart",
        "executionTime": "Execution Time",
        "time": "Time",
        "connectionHoldTime": "Connection Hold Time Chart",
        "holdTime": "Hold Time",
        "transactionDistribution": "Transaction Duration Distribution",
        "transactionCount": "Transaction Count",
        "timeRange": "Time Range",
        "seconds": "s",
        "milliseconds": "ms",
        "connectionAcquiredTooltip": "Average time required to acquire a connection (milliseconds)",
        "connectionTimeoutTooltip": "Ratio of connection acquisition timeouts",
        "highTimeoutRate": "High timeout rate",
        "leakDetectionTooltip": "Connection leak detection threshold, connections not closed after this time will be considered leaked",
        "leakDetectionDisabled": "Leak detection disabled",
        "leakDetectionEnabled": "Leak detection enabled",
        "hikariHealthStatus": "HikariCP Connection Pool Health Status",
        "activeConnectionRatio": "Active/Total Connections Ratio",
        "waitingConnections": "Waiting for Connections",
        "hasWaitingRequests": "Requests waiting for connections",
        "noWaiting": "No waiting",
        "totalSqlExecutions": "Total SQL Executions"
    }
};

const resources = {
    'zh-CN': {
        translation: zhCN,
    },
    'en-US': {
        translation: enUS,
    },
};

i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        resources,
        fallbackLng: 'zh-CN',
        debug: false,

        detection: {
            order: ['localStorage', 'navigator', 'htmlTag'],
            caches: ['localStorage'],
        },

        interpolation: {
            escapeValue: false,
        },
    });

export default i18n;