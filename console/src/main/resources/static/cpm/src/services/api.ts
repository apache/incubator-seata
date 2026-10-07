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
import axios from 'axios';
import {PoolConfig, PoolMetrics, PoolType} from '../types';
import {mockPoolService} from './mockData';

const API_BASE_URL = 'http://localhost:8080/api';

// Create axios instance
const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Toggle whether to use mock data
const USE_MOCK_DATA = true;

// API service
export const poolService = USE_MOCK_DATA ? mockPoolService : {
    // Get all connection pool metrics
    getAllPoolMetrics: async (): Promise<PoolMetrics[]> => {
        try {
            const response = await api.get('/pool/metrics');
            console.log('获取连接池指标成功:', response.data);
            if (response.data && (response.data.code === 200 || response.data.code === 0)) {
                return response.data.data || [];
            }
            return [];
        } catch (error) {
            console.error('获取连接池指标失败:', error);
            return [];
        }
    },

    // Get connection pool metrics by service name
    getPoolMetricsByService: async (serviceName: string): Promise<PoolMetrics> => {
        try {
            const response = await api.get(`/pool/metrics/${serviceName}`);
            if (response.data && (response.data.code === 200 || response.data.code === 0)) {
                return response.data.data;
            }
            throw new Error('获取连接池指标失败');
        } catch (error) {
            console.error(`获取服务 ${serviceName} 的连接池指标失败:`, error);
            throw error;
        }
    },

    // Get connection pool metrics by pool type
    getPoolMetricsByType: async (poolType: PoolType): Promise<PoolMetrics[]> => {
        try {
            const response = await api.get(`/pool/metrics/type/${poolType}`);
            if (response.data && (response.data.code === 200 || response.data.code === 0)) {
                return response.data.data || [];
            }
            return [];
        } catch (error) {
            console.error(`获取类型 ${poolType} 的连接池指标失败:`, error);
            return [];
        }
    },

    // Get all connection pool configurations
    getAllPoolConfigs: async (): Promise<PoolConfig[]> => {
        try {
            const response = await api.get('/pool/config');
            console.log('获取连接池配置成功:', response.data);
            if (response.data && (response.data.code === 200 || response.data.code === 0)) {
                return response.data.data || [];
            }
            return [];
        } catch (error) {
            console.error('获取连接池配置失败:', error);
            return [];
        }
    },

    // Get connection pool configuration by service name
    getPoolConfigByService: async (serviceName: string): Promise<PoolConfig> => {
        try {
            const response = await api.get(`/pool/config/${serviceName}`);
            if (response.data && (response.data.code === 200 || response.data.code === 0)) {
                return response.data.data;
            }
            throw new Error('获取连接池配置失败');
        } catch (error) {
            console.error(`获取服务 ${serviceName} 的连接池配置失败:`, error);
            throw error;
        }
    },

    // Get connection pool configuration by pool type
    getPoolConfigByType: async (poolType: PoolType): Promise<PoolConfig[]> => {
        try {
            const response = await api.get(`/pool/config/type/${poolType}`);
            if (response.data && (response.data.code === 200 || response.data.code === 0)) {
                return response.data.data || [];
            }
            return [];
        } catch (error) {
            console.error(`获取类型 ${poolType} 的连接池配置失败:`, error);
            return [];
        }
    },

    // Update connection pool configuration
    updatePoolConfig: async (serviceName: string, config: any): Promise<boolean> => {
        try {
            const response = await api.put(`/pool/config/${serviceName}`, config);
            console.log('更新连接池配置成功:', response.data);
            if (response.data && (response.data.code === 200 || response.data.code === 0)) {
                return true;
            }
            return false;
        } catch (error) {
            console.error(`更新服务 ${serviceName} 的连接池配置失败:`, error);
            throw error;
        }
    },
};

export default poolService;