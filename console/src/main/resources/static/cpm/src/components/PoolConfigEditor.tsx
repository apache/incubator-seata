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
import {Button, Divider, Form, InputNumber, message, Modal} from 'antd';
import {useTranslation} from 'react-i18next';
import {PoolConfig} from '../types';
import poolService from '../services/api';

interface PoolConfigEditorProps {
    visible: boolean;
    config: PoolConfig | null;
    onCancel: () => void;
    onSuccess: () => void;
}

const PoolConfigEditor: React.FC<PoolConfigEditorProps> = ({
                                                               visible,
                                                               config,
                                                               onCancel,
                                                               onSuccess,
                                                           }) => {
    const {t} = useTranslation();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [originalConfig, setOriginalConfig] = useState<PoolConfig | null>(null);

    useEffect(() => {
        if (visible && config) {
            // Save original config for restore
            setOriginalConfig({...config});
            // Use config data directly since field names already match
            const formValues = {...config};
            // Prefill form
            form.setFieldsValue(formValues);
        }
    }, [visible, config, form]);

    const handleSubmit = async () => {
        try {
            setLoading(true);
            const values = await form.validateFields();

            // Build update request object
            const updateRequest = {
                maxPoolSize: values.maxPoolSize,
                minIdle: values.minIdle,
                connectionTimeout: values.connectionTimeout,
                // Druid-specific fields
                timeBetweenEvictionRunsMills: values.timeBetweenEvictionRunsMills,
                maxEvictableTimeMills: values.maxEvictableTimeMills,
                // HikariCP-specific fields
                maxLifeTime: values.maxLifeTime,
                keepaliveTime: values.keepaliveTime,
            };

            await poolService.updatePoolConfig(config!.serviceName, updateRequest);
            message.success(t('config.updateSuccess'));
            onSuccess();
        } catch (error) {
            message.error(t('config.updateFailed'));
            console.error('配置更新失败:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleReset = () => {
        if (originalConfig) {
            // Use original config data directly since field names already match
            const formValues = {...originalConfig};

            form.setFieldsValue(formValues);
            message.info('已恢复到原始配置');
        }
    };

    const handleCancel = () => {
        form.resetFields();
        onCancel();
    };

    if (!config) return null;

    const isDruid = config.poolType === 'Druid';
    const isHikariCP = config.poolType === 'HikariCP';

    return (
        <Modal
            title={`${t('common.edit')} ${config.serviceName} ${t('config.title')} (${config.poolType})`}
            open={visible}
            onCancel={handleCancel}
            width={600}
            footer={[
                <Button key="reset" onClick={handleReset}>
                    {t('common.refresh')}
                </Button>,
                <Button key="cancel" onClick={handleCancel}>
                    {t('common.cancel')}
                </Button>,
                <Button key="submit" type="primary" loading={loading} onClick={handleSubmit}>
                    {t('common.save')}
                </Button>,
            ]}
        >
            <Form
                form={form}
                layout="vertical"
                initialValues={config}
            >
                <Divider orientation="left">{t('config.basicConfig')}</Divider>

                <Form.Item
                    label={t('config.maxPoolSize')}
                    name="maxPoolSize"
                    rules={[{required: true, message: t('config.maxPoolSize') + ' (1-1000)'}]}
                >
                    <InputNumber min={1} max={1000} style={{width: '100%'}}/>
                </Form.Item>

                <Form.Item
                    label={t('config.minIdle')}
                    name="minIdle"
                    rules={[{required: true, message: t('config.minIdle') + ' (0-1000)'}]}
                >
                    <InputNumber min={0} max={1000} style={{width: '100%'}}/>
                </Form.Item>

                <Form.Item
                    label={t('config.connectionTimeout')}
                    name="connectionTimeout"
                    rules={[{required: true, message: t('config.connectionTimeout') + ' (1000-60000)'}]}
                >
                    <InputNumber min={1000} max={60000} style={{width: '100%'}}/>
                </Form.Item>

                {isDruid && (
                    <>
                        <Divider orientation="left">{t('config.druidSpecific')}</Divider>

                        <Form.Item
                            label={t('config.timeBetweenEvictionRuns')}
                            name="timeBetweenEvictionRunsMills"
                        >
                            <InputNumber min={10000} max={300000} style={{width: '100%'}}/>
                        </Form.Item>

                        <Form.Item
                            label={t('config.maxEvictableTime')}
                            name="maxEvictableTimeMills"
                        >
                            <InputNumber min={60000} max={1800000} style={{width: '100%'}}/>
                        </Form.Item>

                    </>
                )}

                {isHikariCP && (
                    <>
                        <Divider orientation="left">{t('config.hikariSpecific')}</Divider>

                        <Form.Item
                            label={t('config.maxLifeTime')}
                            name="maxLifeTime"
                        >
                            <InputNumber min={600000} max={3600000} style={{width: '100%'}}/>
                        </Form.Item>

                        <Form.Item
                            label={t('config.keepaliveTime')}
                            name="keepaliveTime"
                        >
                            <InputNumber min={0} max={60000} style={{width: '100%'}}/>
                        </Form.Item>
                    </>
                )}
            </Form>
        </Modal>
    );
};

export default PoolConfigEditor;