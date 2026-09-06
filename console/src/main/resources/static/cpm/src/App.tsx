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
import React, {useEffect, useState} from 'react';
import {Layout, Spin, Typography} from 'antd';
import {useTranslation} from 'react-i18next';
import FilterBar from './components/FilterBar';
import MetricsGrid from './components/MetricsGrid';
import PoolTable from './components/PoolTable';
import LanguageToggle from './components/LanguageToggle';
import {PoolConfig, PoolMetrics, PoolType} from './types';
import poolService from './services/api';

const {Header, Content, Footer} = Layout;
const {Title} = Typography;

const App: React.FC = () => {
    const {t} = useTranslation();
    const [loading, setLoading] = useState<boolean>(true);
    const [poolMetrics, setPoolMetrics] = useState<PoolMetrics[]>([]);
    const [poolConfigs, setPoolConfigs] = useState<PoolConfig[]>([]);
    const [filteredMetrics, setFilteredMetrics] = useState<PoolMetrics[]>([]);
    const [selectedPoolType, setSelectedPoolType] = useState<PoolType>('All');
    const [searchText, setSearchText] = useState<string>('');

    // Fetch all connection pool data
    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const [metricsData, configsData] = await Promise.all([
                    poolService.getAllPoolMetrics(),
                    poolService.getAllPoolConfigs()
                ]);
                setPoolMetrics(metricsData);
                setFilteredMetrics(metricsData);
                setPoolConfigs(configsData);
            } catch (error) {
                console.error('Failed to fetch pool data:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    // Apply filtering logic
    useEffect(() => {
        let result = [...poolMetrics];

        // Filter by pool type
        if (selectedPoolType !== 'All') {
            result = result.filter(metric => metric.poolType === selectedPoolType);
        }

        // Search by service name
        if (searchText) {
            result = result.filter(metric =>
                metric.serviceName.toLowerCase().includes(searchText.toLowerCase())
            );
        }

        setFilteredMetrics(result);
    }, [selectedPoolType, searchText, poolMetrics]);

    // Handle pool type changes
    const handlePoolTypeChange = (value: PoolType) => {
        setSelectedPoolType(value);
    };

    // Handle search text changes
    const handleSearchChange = (value: string) => {
        setSearchText(value);
    };

    // Handle config update and refresh data
    const handleConfigUpdate = async () => {
        try {
            setLoading(true);
            const [metricsData, configsData] = await Promise.all([
                poolService.getAllPoolMetrics(),
                poolService.getAllPoolConfigs()
            ]);
            setPoolMetrics(metricsData);
            setFilteredMetrics(metricsData);
            setPoolConfigs(configsData);
        } catch (error) {
            console.error('Failed to refresh pool data:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Layout className="layout" style={{minHeight: '100vh'}}>
            <Header style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                <Title level={3} style={{color: 'white', margin: 0}}>
                    {t('header.title')}
                </Title>
                <LanguageToggle/>
            </Header>
            <Content style={{padding: '0 50px'}}>
                <div className="site-layout-content" style={{margin: '16px 0'}}>
                    <FilterBar
                        selectedPoolType={selectedPoolType}
                        searchText={searchText}
                        onPoolTypeChange={handlePoolTypeChange}
                        onSearchChange={handleSearchChange}
                    />

                    {loading ? (
                        <div style={{textAlign: 'center', padding: '50px'}}>
                            <Spin size="large">
                                <div style={{padding: '20px'}}>{t('common.loading')}</div>
                            </Spin>
                        </div>
                    ) : (
                        <>
                            <MetricsGrid metrics={filteredMetrics}/>
                            <PoolTable
                                metrics={filteredMetrics}
                                configs={poolConfigs}
                                onConfigUpdate={handleConfigUpdate}
                            />
                        </>
                    )}
                </div>
            </Content>
            <Footer style={{textAlign: 'center'}}>
                Connection Pool Monitoring System ©{new Date().getFullYear()}
            </Footer>
        </Layout>
    );
};

export default App;