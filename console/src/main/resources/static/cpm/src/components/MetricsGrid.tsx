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
import React from 'react';
import {Badge, Card, Col, Row, Statistic, Tooltip} from 'antd';
import {ClockCircleOutlined, HourglassOutlined, ThunderboltOutlined, UserOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';
import {PoolMetrics} from '../types';

interface MetricsGridProps {
    metrics: PoolMetrics[];
}

const MetricsGrid: React.FC<MetricsGridProps> = ({metrics}) => {
    const {t} = useTranslation();
    // Calculate aggregated totals
    const totalActive = metrics.reduce((sum, metric) => sum + metric.activeConnections, 0);
    const totalIdle = metrics.reduce((sum, metric) => sum + metric.idleConnections, 0);
    const totalWait = metrics.reduce((sum, metric) => sum + metric.waitThreadCount, 0);
    const totalExecute = metrics.reduce((sum, metric) => sum + metric.executeCount, 0);
    // Get color by pool type
    const getPoolTypeColor = (poolType: string) => {
        switch (poolType) {
            case 'Druid':
                return '#1890ff';
            case 'HikariCP':
                return '#52c41a';
            default:
                return '#722ed1';
        }
    };

    return (
        <div>
            <h2>{t('charts.performanceMetrics')}</h2>

            {/* Aggregated metric cards */}
            <Card className="metric-card" style={{marginBottom: '24px'}}>
                <Row gutter={16}>
                    <Col span={6}>
                        <Statistic
                            title={t('metrics.totalActiveConnections')}
                            value={totalActive}
                            prefix={<UserOutlined/>}
                            valueStyle={{color: '#1890ff'}}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title={t('metrics.totalIdleConnections')}
                            value={totalIdle}
                            prefix={<ClockCircleOutlined/>}
                            valueStyle={{color: '#52c41a'}}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title={t('metrics.totalWaitThreadCount')}
                            value={totalWait}
                            prefix={<HourglassOutlined/>}
                            valueStyle={{color: totalWait > 0 ? '#faad14' : '#8c8c8c'}}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title={t('metrics.totalExecuteCount')}
                            value={totalExecute}
                            prefix={<ThunderboltOutlined/>}
                            valueStyle={{color: '#722ed1'}}
                        />
                    </Col>
                </Row>
            </Card>

            {/* Per-service metric card grid */}
            <div className="card-container">
                {metrics.map((metric) => (
                    <Card
                        key={metric.serviceName}
                        title={
                            <Tooltip title={`${t('table.poolType')}: ${metric.poolType}`}>
                <span>
                  <Badge
                      color={getPoolTypeColor(metric.poolType)}
                      text={metric.serviceName}
                  />
                </span>
                            </Tooltip>
                        }
                        className="metric-card"
                        hoverable
                    >
                        <Row gutter={[16, 16]}>
                            <Col span={12}>
                                <Statistic
                                    title={t('table.activeConnections')}
                                    value={metric.activeConnections}
                                    valueStyle={{color: '#1890ff'}}
                                />
                            </Col>
                            <Col span={12}>
                                <Statistic
                                    title={t('table.idleConnections')}
                                    value={metric.idleConnections}
                                    valueStyle={{color: '#52c41a'}}
                                />
                            </Col>
                            <Col span={12}>
                                <Statistic
                                    title={t('metrics.waitThreadCount')}
                                    value={metric.waitThreadCount}
                                    valueStyle={{color: metric.waitThreadCount > 0 ? '#faad14' : '#8c8c8c'}}
                                />
                            </Col>
                            <Col span={12}>
                                <Statistic
                                    title={t('druid.executeCount')}
                                    value={metric.executeCount}
                                    valueStyle={{color: '#722ed1'}}
                                />
                            </Col>
                        </Row>
                    </Card>
                ))}
            </div>
        </div>
    );
};

export default MetricsGrid;