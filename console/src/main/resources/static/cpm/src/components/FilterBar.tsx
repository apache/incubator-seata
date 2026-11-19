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
import {Input, Select, Space, Typography} from 'antd';
import {SearchOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';
import {PoolType} from '../types';

const {Option} = Select;
const {Title} = Typography;

interface FilterBarProps {
    selectedPoolType: PoolType;
    searchText: string;
    onPoolTypeChange: (value: PoolType) => void;
    onSearchChange: (value: string) => void;
}

const FilterBar: React.FC<FilterBarProps> = ({
                                                 selectedPoolType,
                                                 searchText,
                                                 onPoolTypeChange,
                                                 onSearchChange,
                                             }) => {
    const {t} = useTranslation();

    return (
        <div className="filter-container">
            <Title level={4} style={{margin: 0, marginRight: '16px'}}>
                {t('filter.title')}
            </Title>
            <Space size="middle">
                <div>
                    <span style={{marginRight: '8px'}}>{t('filter.poolType')}</span>
                    <Select
                        value={selectedPoolType}
                        onChange={onPoolTypeChange}
                        style={{width: 150}}
                    >
                        <Option value="All">{t('common.all')}</Option>
                        <Option value="Druid">Druid</Option>
                        <Option value="HikariCP">HikariCP</Option>
                    </Select>
                </div>
                <div>
                    <Input
                        placeholder={t('filter.searchPlaceholder')}
                        value={searchText}
                        onChange={(e) => onSearchChange(e.target.value)}
                        style={{width: 200}}
                        prefix={<SearchOutlined/>}
                        allowClear
                    />
                </div>
            </Space>
        </div>
    );
};

export default FilterBar;