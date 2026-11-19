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
import type {MenuProps} from 'antd';
import {Button, Dropdown, Space} from 'antd';
import {GlobalOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';

const LanguageToggle: React.FC = () => {
    const {i18n} = useTranslation();

    const currentLanguage = i18n.language;

    const handleLanguageChange = (language: string) => {
        i18n.changeLanguage(language);
    };

    const items: MenuProps['items'] = [
        {
            key: 'zh-CN',
            label: (
                <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <span>🇨🇳</span>
                    <span>中文</span>
                    {currentLanguage === 'zh-CN' && <span style={{color: '#1890ff'}}>✓</span>}
                </div>
            ),
            onClick: () => handleLanguageChange('zh-CN'),
        },
        {
            key: 'en-US',
            label: (
                <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <span>🇺🇸</span>
                    <span>English</span>
                    {currentLanguage === 'en-US' && <span style={{color: '#1890ff'}}>✓</span>}
                </div>
            ),
            onClick: () => handleLanguageChange('en-US'),
        },
    ];

    // Return current language display label
    const getCurrentLanguageDisplay = () => {
        switch (currentLanguage) {
            case 'zh-CN':
                return '中文';
            case 'en-US':
                return 'English';
            default:
                return '中文';
        }
    };

    return (
        <Dropdown
            menu={{items}}
            placement="bottomRight"
            trigger={['click']}
            overlayStyle={{minWidth: '120px'}}
        >
            <Button
                type="text"
                icon={<GlobalOutlined/>}
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    color: '#666',
                    fontSize: '14px',
                }}
            >
                <Space>
                    {getCurrentLanguageDisplay()}
                </Space>
            </Button>
        </Dropdown>
    );
};

export default LanguageToggle;