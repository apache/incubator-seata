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
import React, { useState } from "react";
import { Badge, Button, Card, Descriptions, Space, Table, Tabs } from "antd";
import { EditOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { PoolConfig, PoolMetrics } from "../types";
import DruidCharts from "./charts/DruidCharts";
import HikariCPMetricsPanel from "./charts/HikariCPMetricsPanel";
import PoolConfigEditor from "./PoolConfigEditor";

const { TabPane } = Tabs;

interface PoolTableProps {
  metrics: PoolMetrics[];
  configs: PoolConfig[];
  onConfigUpdate?: () => void;
}

const PoolTable: React.FC<PoolTableProps> = ({
  metrics,
  configs,
  onConfigUpdate = () => {},
}) => {
  const { t } = useTranslation();
  const [editingConfig, setEditingConfig] = useState<PoolConfig | null>(null);
  const [editorVisible, setEditorVisible] = useState(false);
  // Get configuration by service name
  const getConfigForService = (serviceName: string): PoolConfig | undefined => {
    return configs.find((config) => config.serviceName === serviceName);
  };
  // Handle edit configuration action
  const handleEditConfig = (serviceName: string) => {
    const config = getConfigForService(serviceName);
    const metric = metrics.find((m) => m.serviceName === serviceName);
    if (config && metric) {
      // Add poolType from metrics into config
      const configWithPoolType = {
        ...config,
        poolType: metric.poolType,
      };
      setEditingConfig(configWithPoolType);
      setEditorVisible(true);
    }
  };
  // Handle editor close
  const handleEditorCancel = () => {
    setEditorVisible(false);
    setEditingConfig(null);
  };
  // Handle successful config update
  const handleConfigUpdateSuccess = () => {
    setEditorVisible(false);
    setEditingConfig(null);
    if (onConfigUpdate) {
      onConfigUpdate();
    }
  };
  // Get badge status by pool type
  const getPoolTypeBadgeStatus = (poolType: string) => {
    switch (poolType) {
      case "Druid":
        return "processing";
      case "HikariCP":
        return "success";
      default:
        return "default";
    }
  };
  // Table column definitions
  const columns = [
    {
      title: t("table.serviceName"),
      dataIndex: "serviceName",
      key: "serviceName",
      sorter: (a: PoolMetrics, b: PoolMetrics) =>
        a.serviceName.localeCompare(b.serviceName),
    },
    {
      title: t("table.poolType"),
      dataIndex: "poolType",
      key: "poolType",
      render: (poolType: string) => (
        <Badge
          status={getPoolTypeBadgeStatus(poolType) as any}
          text={poolType}
        />
      ),
      filters: [
        { text: "Druid", value: "Druid" },
        { text: "HikariCP", value: "HikariCP" },
      ],
    },
    {
      title: t("table.activeConnections"),
      dataIndex: "activeConnections",
      key: "activeConnections",
      sorter: (a: PoolMetrics, b: PoolMetrics) =>
        a.activeConnections - b.activeConnections,
    },
    {
      title: t("table.idleConnections"),
      dataIndex: "idleConnections",
      key: "idleConnections",
      sorter: (a: PoolMetrics, b: PoolMetrics) =>
        a.idleConnections - b.idleConnections,
    },
    {
      title: t("metrics.waitThreadCount"),
      dataIndex: "waitThreadCount",
      key: "waitThreadCount",
      sorter: (a: PoolMetrics, b: PoolMetrics) =>
        a.waitThreadCount - b.waitThreadCount,
    },
    {
      title: t("druid.executeCount"),
      dataIndex: "executeCount",
      key: "executeCount",
      sorter: (a: PoolMetrics, b: PoolMetrics) =>
        a.executeCount - b.executeCount,
    },
    {
      title: t("table.actions"),
      key: "action",
      render: (_, record: PoolMetrics) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEditConfig(record.serviceName)}
          >
            {t("table.editConfig")}
          </Button>
        </Space>
      ),
    },
  ];

  // Expand row rendering function
  const expandedRowRender = (record: PoolMetrics) => {
    const config = getConfigForService(record.serviceName);

    return (
      <Tabs defaultActiveKey="config">
        <TabPane tab={t("table.config")} key="config">
          <Descriptions bordered column={2} size="small">
            {config &&
              Object.entries(config)
                .filter(([key]) => key !== "serviceName" && key !== "poolType")
                .map(([key, value]) => (
                  <Descriptions.Item key={key} label={key}>
                    {value !== undefined ? String(value) : "-"}
                  </Descriptions.Item>
                ))}
          </Descriptions>
        </TabPane>

        {record.poolType === "Druid" && (
          <TabPane tab={t("charts.performanceMetrics")} key="druid-charts">
            <DruidCharts metrics={record as PoolMetrics} />
          </TabPane>
        )}

        {record.poolType === "HikariCP" && (
          <TabPane tab={t("charts.performanceMetrics")} key="hikaricp-metrics">
            <HikariCPMetricsPanel metrics={record as PoolMetrics} />
          </TabPane>
        )}
      </Tabs>
    );
  };

  return (
    <div style={{ marginTop: "24px" }}>
      <h2>{t("table.details")}</h2>
      <Card>
        <Table
          columns={columns}
          dataSource={metrics}
          rowKey="serviceName"
          expandable={{
            expandedRowRender,
          }}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <PoolConfigEditor
        visible={editorVisible}
        config={editingConfig}
        onCancel={handleEditorCancel}
        onSuccess={handleConfigUpdateSuccess}
      />
    </div>
  );
};

export default PoolTable;
