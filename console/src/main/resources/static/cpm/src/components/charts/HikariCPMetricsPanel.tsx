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
import {Card, Col, Divider, Progress, Row, Statistic, Tooltip} from 'antd';
import {ClockCircleOutlined, InfoCircleOutlined, WarningOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';
import {PoolMetrics} from '../../types';

interface HikariCPMetricsPanelProps {
    metrics: PoolMetrics;
}

const HikariCPMetricsPanel: React.FC<HikariCPMetricsPanelProps> = ({metrics}) => {
    const {t} = useTranslation();
    // Convert nanoseconds to milliseconds for display
    const connectionAcquiredMs = metrics.connectionAcquiredNanos / 1000000;
    // Calculate timeout rate percentage (ensure within 0-100)
    const timeoutRatePercent = Math.min(metrics.connectionTimeoutRate * 100, 100);
    // Determine severity based on timeout rate
    const getTimeoutRateSeverity = (rate: number) => {
        if (rate < 0.01) return 'success';
        if (rate < 0.05) return 'normal';
        if (rate < 0.1) return 'exception';
        return 'exception';
    };
    // Format leak detection threshold into a friendly display (ms to s)
    const formatLeakThreshold = (ms: number) => {
        if (ms >= 1000) {
            return `${(ms / 1000).toFixed(1)}${t('charts.seconds')}`;
        }
        return `${ms}${t('charts.milliseconds')}`;
    };

    return (
        <div>
            <Row gutter={[16, 16]}>
                <Col span={8}>
                    <Card>
                        <Statistic
                            title={
                                <Tooltip title={t('charts.connectionAcquiredTooltip')}>
                  <span>
                    {t('hikaricp.connectionAcquiredNanos')} <InfoCircleOutlined/>
                  </span>
                                </Tooltip>
                            }
                            value={connectionAcquiredMs.toFixed(2)}
                            suffix="ms"
                            precision={2}
                            valueStyle={{color: connectionAcquiredMs > 100 ? '#cf1322' : '#3f8600'}}
                            prefix={<ClockCircleOutlined/>}
                        />
                    </Card>
                </Col>

                <Col span={8}>
                    <Card>
                        <div style={{textAlign: 'center'}}>
                            <Tooltip title={t('charts.connectionTimeoutTooltip')}>
                                <h4>
                                    {t('hikaricp.connectionTimeoutRate')} <InfoCircleOutlined/>
                                </h4>
                            </Tooltip>
                            <Progress
                                type="dashboard"
                                percent={timeoutRatePercent}
                                status={getTimeoutRateSeverity(metrics.connectionTimeoutRate) as any}
                                format={(percent) => `${(percent! / 100).toFixed(4)}`}
                            />
                            <div style={{marginTop: '8px'}}>
                                {metrics.connectionTimeoutRate > 0.05 && (
                                    <span style={{color: '#cf1322'}}>
                    <WarningOutlined/> {t('charts.highTimeoutRate')}
                  </span>
                                )}
                            </div>
                        </div>
                    </Card>
                </Col>

                <Col span={8}>
                    <Card>
                        <Statistic
                            title={
                                <Tooltip title={t('charts.leakDetectionTooltip')}>
                  <span>
                    {t('hikaricp.leakDetectionThreshold')} <InfoCircleOutlined/>
                  </span>
                                </Tooltip>
                            }
                            value={formatLeakThreshold(metrics.leakDetectionThreshold)}
                            valueStyle={{color: '#1890ff'}}
                        />
                        <Divider style={{margin: '12px 0'}}/>
                        <div>
                            {metrics.leakDetectionThreshold === 0 ? (
                                <span style={{color: '#faad14'}}>
                  <WarningOutlined/> {t('charts.leakDetectionDisabled')}
                </span>
                            ) : (
                                <span style={{color: '#52c41a'}}>{t('charts.leakDetectionEnabled')}</span>
                            )}
                        </div>
                    </Card>
                </Col>
            </Row>

            <Row style={{marginTop: '16px'}}>
                <Col span={24}>
                    <Card title={t('charts.hikariHealthStatus')}>
                        <p>
                            <strong>{t('charts.activeConnectionRatio')}：</strong>{' '}
                            <Progress
                                percent={Math.round((metrics.activeConnections / metrics.totalConnections) * 100) || 0}
                                status={metrics.activeConnections > metrics.totalConnections * 0.8 ? 'exception' : 'normal'}
                            />
                        </p>
                        <p>
                            <strong>{t('charts.waitingConnections')}：</strong>{' '}
                            {metrics.waitThreadCount > 0 ? (
                                <span style={{color: '#cf1322'}}>
                  {metrics.waitThreadCount} <WarningOutlined/> {t('charts.hasWaitingRequests')}
                </span>
                            ) : (
                                <span style={{color: '#52c41a'}}>0 ({t('charts.noWaiting')})</span>
                            )}
                        </p>
                        <p>
                            <strong>{t('charts.totalSqlExecutions')}：</strong> {metrics.executeCount}
                        </p>
                    </Card>
                </Col>
            </Row>
        </div>
    );
};

export default HikariCPMetricsPanel;