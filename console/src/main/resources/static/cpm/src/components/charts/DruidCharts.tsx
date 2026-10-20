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
import React from "react";
import { Card, Col, Empty, Row } from "antd";
import { useTranslation } from "react-i18next";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { PoolMetrics } from "../../types";

interface DruidChartsProps {
  metrics: PoolMetrics;
}

const DruidCharts: React.FC<DruidChartsProps> = ({ metrics }) => {
  const { t } = useTranslation();

  // Format SQL execution data for line chart
  const formatSqlExecutionData = () => {
    if (
      !metrics.sqlExecutionRecord ||
      metrics.sqlExecutionRecord.length === 0
    ) {
      return [];
    }

    // Sort by timestamp
    const sortedRecords = [...metrics.sqlExecutionRecord].sort(
      (a, b) =>
        new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    // Convert to chart data format
    return sortedRecords.map((record) => ({
      time: new Date(record.timestamp).toLocaleTimeString(),
      executeTime: record.executionTimeMillis,
      sql: record.sql.substring(0, 30) + (record.sql.length > 30 ? "..." : ""),
    }));
  };

  // Format connection hold time data for line chart
  const formatConnectionHoldTimeData = () => {
    if (
      !metrics.sqlExecutionRecord ||
      metrics.sqlExecutionRecord.length === 0
    ) {
      return [];
    }

    // Sort by timestamp
    const sortedRecords = [...metrics.sqlExecutionRecord].sort(
      (a, b) =>
        new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    // Convert to chart data format
    return sortedRecords.map((record) => ({
      time: new Date(record.timestamp).toLocaleTimeString(),
      holdTime: record.holdTimeMillis,
      sql: record.sql.substring(0, 30) + (record.sql.length > 30 ? "..." : ""),
    }));
  };

  // Format transaction duration histogram data for bar chart
  const formatTransactionHistogramData = () => {
    if (
      !metrics.transactionHistogramValues ||
      !metrics.transactionHistogramRanges ||
      metrics.transactionHistogramValues.length === 0 ||
      metrics.transactionHistogramRanges.length === 0
    ) {
      return [];
    }

    // Convert to chart data format
    return metrics.transactionHistogramRanges.map((range, index) => ({
      range,
      count: metrics.transactionHistogramValues[index] || 0,
    }));
  };

  const sqlExecutionData = formatSqlExecutionData();
  const connectionHoldTimeData = formatConnectionHoldTimeData();
  const transactionHistogramData = formatTransactionHistogramData();

  // Check if there is data to display
  const hasData =
    sqlExecutionData.length > 0 ||
    connectionHoldTimeData.length > 0 ||
    transactionHistogramData.length > 0;

  if (!hasData) {
    return (
      <Empty
        description={t("charts.noData")}
        image={Empty.PRESENTED_IMAGE_SIMPLE}
      />
    );
  }

  return (
    <div>
      <Row gutter={[16, 16]}>
        {/* SQL执行时间折线图 */}
        {/* SQL Execution Time line chart */}
        <Col span={24}>
          <Card
            title={t("charts.sqlExecutionTime")}
            className="chart-container"
          >
            <ResponsiveContainer width="100%" height={300}>
              <LineChart
                data={sqlExecutionData}
                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip
                  formatter={(value: any, name: any) => [
                    `${value} ms`,
                    t("charts.executionTime"),
                  ]}
                  labelFormatter={(label) => `${t("charts.time")}: ${label}`}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="executeTime"
                  name={t("charts.sqlExecutionTime")}
                  stroke="#1890ff"
                  activeDot={{ r: 8 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </Card>
        </Col>

        {/* Connection Hold Time line chart */}
        <Col span={24}>
          <Card
            title={t("charts.connectionHoldTime")}
            className="chart-container"
          >
            <ResponsiveContainer width="100%" height={300}>
              <LineChart
                data={connectionHoldTimeData}
                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip
                  formatter={(value: any, name: any) => [
                    `${value} ms`,
                    t("charts.holdTime"),
                  ]}
                  labelFormatter={(label) => `${t("charts.time")}: ${label}`}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="holdTime"
                  name={t("charts.connectionHoldTime")}
                  stroke="#52c41a"
                  activeDot={{ r: 8 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </Card>
        </Col>

        {/* Transaction duration distribution bar chart */}
        <Col span={24}>
          <Card
            title={t("charts.transactionDistribution")}
            className="chart-container"
          >
            <ResponsiveContainer width="100%" height={300}>
              <BarChart
                data={transactionHistogramData}
                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="range" />
                <YAxis />
                <Tooltip
                  formatter={(value: any, name: any) => [
                    value,
                    t("charts.transactionCount"),
                  ]}
                  labelFormatter={(label) =>
                    `${t("charts.timeRange")}: ${label}`
                  }
                />
                <Legend />
                <Bar
                  dataKey="count"
                  name={t("charts.transactionCount")}
                  fill="#722ed1"
                />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DruidCharts;
