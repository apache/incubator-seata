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
import { ConfigProvider, Table, Button, Form, Icon, Dialog, Select, Input, Message } from '@alicloud/console-components';
import Actions from '@alicloud/console-components-actions';
import { withRouter } from 'react-router-dom';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import {
  BusinessDataSourceForm,
  BusinessDataSourceInfo,
  fetchBusinessDataSources,
  registerBusinessDataSource,
  testBusinessDataSource,
  unregisterBusinessDataSource,
} from '@/service/businessDataSource';

import './index.scss';

const FormItem = Form.Item;

type BusinessDataSourceLocale = {
  title?: string;
  subTitle?: string;
  keywordLabel?: string;
  keywordPlaceholder?: string;
  resetButtonLabel?: string;
  searchButtonLabel?: string;
  refreshButtonLabel?: string;
  createButtonLabel?: string;
  createDialogTitle?: string;
  testButtonLabel?: string;
  confirmButtonLabel?: string;
  cancelButtonLabel?: string;
  nameLabel?: string;
  urlLabel?: string;
  usernameLabel?: string;
  passwordLabel?: string;
  passwordSecretRefLabel?: string;
  datasourceLabel?: string;
  minConnLabel?: string;
  maxConnLabel?: string;
  maxWaitLabel?: string;
  databaseNameLabel?: string;
  enabledLabel?: string;
  dynamicLabel?: string;
  operations?: string;
  deleteLabel?: string;
  staticSourceLabel?: string;
  yesLabel?: string;
  noLabel?: string;
  successLabel?: string;
  failedLabel?: string;
  validationRequiredMessage?: string;
  passwordRequiredMessage?: string;
  createSuccessMessage?: string;
  testSuccessMessage?: string;
  testFailedMessage?: string;
  deleteTitle?: string;
  deleteContent?: string;
  deleteSuccessMessage?: string;
};

type BusinessDataSourceState = {
  list: BusinessDataSourceInfo[];
  keyword: string;
  loading: boolean;
  dialogVisible: boolean;
  dialogLoading: boolean;
  testLoading: boolean;
  form: BusinessDataSourceForm;
};

const defaultForm: BusinessDataSourceForm = {
  name: '',
  url: '',
  username: '',
  password: '',
  passwordSecretRef: '',
  datasource: 'druid',
  minConn: 10,
  maxConn: 100,
  maxWait: 5000,
};

class BusinessDataSource extends React.Component<GlobalProps, BusinessDataSourceState> {
  static displayName = 'BusinessDataSource';

  static propTypes = {
    locale: PropTypes.object,
  };

  state: BusinessDataSourceState = {
    list: [],
    keyword: '',
    loading: false,
    dialogVisible: false,
    dialogLoading: false,
    testLoading: false,
    form: { ...defaultForm },
  };

  componentDidMount = () => {
    this.loadDataSources();
  };

  loadDataSources = async () => {
    this.setState(prevState => ({
      ...prevState,
      loading: true,
    }));
    try {
      const list = await fetchBusinessDataSources();
      this.setState(prevState => ({
        ...prevState,
        list,
        loading: false,
      }));
    } catch (error) {
      this.setState(prevState => ({
        ...prevState,
        loading: false,
      }));
    }
  };

  searchFilterOnChange = (value: string) => {
    this.setState(prevState => ({
      ...prevState,
      keyword: value,
    }));
  };

  resetSearchFilter = () => {
    this.setState(prevState => ({
      ...prevState,
      keyword: '',
    }));
  };

  showCreateDialog = () => {
    this.setState(prevState => ({
      ...prevState,
      dialogVisible: true,
      form: { ...defaultForm },
    }));
  };

  closeCreateDialog = () => {
    this.setState(prevState => ({
      ...prevState,
      dialogVisible: false,
      dialogLoading: false,
      testLoading: false,
    }));
  };

  formOnChange = (key: keyof BusinessDataSourceForm, value: string | number) => {
    this.setState(prevState => ({
      ...prevState,
      form: {
        ...prevState.form,
        [key]: value,
      },
    }));
  };

  numberFormOnChange = (key: keyof BusinessDataSourceForm, value: string) => {
    const nextValue = Number(value);
    this.formOnChange(key, Number.isNaN(nextValue) ? 0 : nextValue);
  };

  validateForm = () => {
    const { locale } = this.props;
    const rawLocale = locale.BusinessDataSource;
    const businessDataSourceLocale: BusinessDataSourceLocale = typeof rawLocale === 'object' && rawLocale !== null ? rawLocale : {};
    const { validationRequiredMessage, passwordRequiredMessage } = businessDataSourceLocale;
    const { name, url, username, password, passwordSecretRef } = this.state.form;
    if (!name || !url || !username) {
      Message.error(validationRequiredMessage || 'Please enter name, URL and username');
      return false;
    }
    if (!password && !passwordSecretRef) {
      Message.error(passwordRequiredMessage || 'Please enter password or password secret ref');
      return false;
    }
    return true;
  };

  showErrorMessage = (error: any) => {
    if (error && error.message) {
      Message.error(error.message);
    }
  };

  createDataSource = async () => {
    const { locale } = this.props;
    const rawLocale = locale.BusinessDataSource;
    const businessDataSourceLocale: BusinessDataSourceLocale = typeof rawLocale === 'object' && rawLocale !== null ? rawLocale : {};
    const { createSuccessMessage } = businessDataSourceLocale;
    if (!this.validateForm()) {
      return;
    }
    this.setState(prevState => ({
      ...prevState,
      dialogLoading: true,
    }));
    try {
      await registerBusinessDataSource(this.state.form);
      Message.success(createSuccessMessage || 'Data source registered successfully');
      this.closeCreateDialog();
      this.loadDataSources();
    } catch (error) {
      this.showErrorMessage(error);
      this.setState(prevState => ({
        ...prevState,
        dialogLoading: false,
      }));
    }
  };

  testDataSource = async () => {
    const { locale } = this.props;
    const rawLocale = locale.BusinessDataSource;
    const businessDataSourceLocale: BusinessDataSourceLocale = typeof rawLocale === 'object' && rawLocale !== null ? rawLocale : {};
    const { testSuccessMessage, testFailedMessage } = businessDataSourceLocale;
    if (!this.validateForm()) {
      return;
    }
    this.setState(prevState => ({
      ...prevState,
      testLoading: true,
    }));
    try {
      const result = await testBusinessDataSource(this.state.form);
      const detail = result && result.message ? `: ${result.message}` : '';
      if (result && result.success) {
        Message.success(`${testSuccessMessage || 'Connection test succeeded'}${detail}`);
      } else {
        Message.error(`${testFailedMessage || 'Connection test failed'}${detail}`);
      }
    } catch (error) {
      this.showErrorMessage(error);
    } finally {
      this.setState(prevState => ({
        ...prevState,
        testLoading: false,
      }));
    }
  };

  deleteDataSource = (record: BusinessDataSourceInfo) => {
    const { locale } = this.props;
    const rawLocale = locale.BusinessDataSource;
    const businessDataSourceLocale: BusinessDataSourceLocale = typeof rawLocale === 'object' && rawLocale !== null ? rawLocale : {};
    const { deleteTitle, deleteContent, deleteSuccessMessage } = businessDataSourceLocale;
    Dialog.confirm({
      title: deleteTitle || 'Delete data source',
      content: `${deleteContent || 'Confirm to delete dynamic data source'}: ${record.name}`,
      onOk: async () => {
        await unregisterBusinessDataSource(record.name);
        Message.success(deleteSuccessMessage || 'Data source deleted successfully');
        this.loadDataSources();
      },
    });
  };

  getFilteredList = () => {
    const { keyword, list } = this.state;
    const normalizedKeyword = keyword.trim().toLowerCase();
    if (!normalizedKeyword) {
      return list;
    }
    return list.filter(item => {
      return [item.name, item.resourceId, item.databaseName, item.datasource]
        .filter(Boolean)
        .some(value => String(value).toLowerCase().indexOf(normalizedKeyword) >= 0);
    });
  };

  render() {
    const { locale } = this.props;
    const rawLocale = locale.BusinessDataSource;
    const businessDataSourceLocale: BusinessDataSourceLocale = typeof rawLocale === 'object' && rawLocale !== null ? rawLocale : {};
    const {
      title,
      subTitle,
      keywordLabel,
      keywordPlaceholder,
      resetButtonLabel,
      searchButtonLabel,
      refreshButtonLabel,
      createButtonLabel,
      createDialogTitle,
      testButtonLabel,
      confirmButtonLabel,
      cancelButtonLabel,
      nameLabel,
      urlLabel,
      usernameLabel,
      passwordLabel,
      passwordSecretRefLabel,
      datasourceLabel,
      minConnLabel,
      maxConnLabel,
      maxWaitLabel,
      databaseNameLabel,
      enabledLabel,
      dynamicLabel,
      operations,
      deleteLabel,
      staticSourceLabel,
      yesLabel,
      noLabel,
    } = businessDataSourceLocale;
    const filteredList = this.getFilteredList();
    return (
      <Page
        title={title || 'Business Data Source'}
        breadcrumbs={[
          {
            link: '/',
            text: title || 'Business Data Source',
          },
          {
            text: subTitle || 'Data source configuration',
          },
        ]}
      >
        <Form inline labelAlign="left" className="business-datasource-toolbar">
          <FormItem name="keyword" label={keywordLabel || 'Keyword'}>
            <Input
              placeholder={keywordPlaceholder || 'Search name, resourceId, database or datasource'}
              value={this.state.keyword}
              onChange={(value: string) => { this.searchFilterOnChange(value); }}
            />
          </FormItem>
          <FormItem>
            <Form.Reset onClick={this.resetSearchFilter}>
              <Icon type="redo" />{resetButtonLabel || 'Reset'}
            </Form.Reset>
          </FormItem>
          <FormItem>
            <Form.Submit onClick={this.loadDataSources}>
              <Icon type="search" />{searchButtonLabel || 'Search'}
            </Form.Submit>
          </FormItem>
          <FormItem>
            <Button onClick={this.loadDataSources}>
              <Icon type="refresh" />{refreshButtonLabel || 'Refresh'}
            </Button>
          </FormItem>
          <FormItem>
            <Button type="primary" onClick={this.showCreateDialog}>
              <Icon type="add" />{createButtonLabel || 'Create'}
            </Button>
          </FormItem>
        </Form>

        <Table dataSource={filteredList} loading={this.state.loading}>
          <Table.Column title={nameLabel || 'Name'} dataIndex="name" />
          <Table.Column title="resourceId" dataIndex="resourceId" />
          <Table.Column title={databaseNameLabel || 'Database'} dataIndex="databaseName" />
          <Table.Column title={datasourceLabel || 'Datasource'} dataIndex="datasource" />
          <Table.Column title={enabledLabel || 'Enabled'} dataIndex="enabled" cell={(value: boolean) => (value ? (yesLabel || 'Yes') : (noLabel || 'No'))} />
          <Table.Column title={dynamicLabel || 'Dynamic'} dataIndex="dynamic" cell={(value: boolean) => (value ? (yesLabel || 'Yes') : (noLabel || 'No'))} />
          <Table.Column
            title={operations || 'Operations'}
            cell={(value: any, index: number, record: BusinessDataSourceInfo) => {
              return (
                <Actions>
                  {record.dynamic ? (
                    <Button warning onClick={() => this.deleteDataSource(record)}>
                      {deleteLabel || 'Delete'}
                    </Button>
                  ) : (
                    <span>{staticSourceLabel || 'Static'}</span>
                  )}
                </Actions>
              );
            }}
          />
        </Table>

        <Dialog
          visible={this.state.dialogVisible}
          title={createDialogTitle || 'Create data source'}
          onClose={this.closeCreateDialog}
          onOk={this.createDataSource}
          okProps={{ loading: this.state.dialogLoading }}
          okText={confirmButtonLabel || 'Confirm'}
          cancelText={cancelButtonLabel || 'Cancel'}
          style={{ width: '640px' }}
        >
          <Form labelAlign="left" className="business-datasource-dialog-form">
            <FormItem name="name" label={nameLabel || 'Name'} required>
              <Input value={this.state.form.name} onChange={(value: string) => { this.formOnChange('name', value); }} />
            </FormItem>
            <FormItem name="url" label={urlLabel || 'JDBC URL'} required>
              <Input
                placeholder="jdbc:mysql://127.0.0.1:3306/database"
                value={this.state.form.url}
                onChange={(value: string) => { this.formOnChange('url', value); }}
              />
            </FormItem>
            <FormItem name="username" label={usernameLabel || 'Username'} required>
              <Input value={this.state.form.username} onChange={(value: string) => { this.formOnChange('username', value); }} />
            </FormItem>
            <FormItem name="password" label={passwordLabel || 'Password'}>
              <Input
                htmlType="password"
                value={this.state.form.password}
                onChange={(value: string) => { this.formOnChange('password', value); }}
              />
            </FormItem>
            <FormItem name="passwordSecretRef" label={passwordSecretRefLabel || 'Password Secret Ref'}>
              <Input
                value={this.state.form.passwordSecretRef}
                onChange={(value: string) => { this.formOnChange('passwordSecretRef', value); }}
              />
            </FormItem>
            <FormItem name="datasource" label={datasourceLabel || 'Datasource'}>
              <Select
                value={this.state.form.datasource}
                dataSource={[
                  { label: 'druid', value: 'druid' },
                  { label: 'hikari', value: 'hikari' },
                  { label: 'dbcp', value: 'dbcp' },
                ]}
                onChange={(value: string) => { this.formOnChange('datasource', value); }}
              />
            </FormItem>
            <FormItem name="minConn" label={minConnLabel || 'Min Connections'}>
              <Input value={String(this.state.form.minConn)} onChange={(value: string) => { this.numberFormOnChange('minConn', value); }} />
            </FormItem>
            <FormItem name="maxConn" label={maxConnLabel || 'Max Connections'}>
              <Input value={String(this.state.form.maxConn)} onChange={(value: string) => { this.numberFormOnChange('maxConn', value); }} />
            </FormItem>
            <FormItem name="maxWait" label={maxWaitLabel || 'Max Wait(ms)'}>
              <Input value={String(this.state.form.maxWait)} onChange={(value: string) => { this.numberFormOnChange('maxWait', value); }} />
            </FormItem>
            <FormItem>
              <Button loading={this.state.testLoading} onClick={this.testDataSource}>
                {testButtonLabel || 'Test Connection'}
              </Button>
            </FormItem>
          </Form>
        </Dialog>
      </Page>
    );
  }
}

const mapStateToProps = (state: any) => ({
  locale: state.locale.locale,
});

export default connect(mapStateToProps)(withRouter(ConfigProvider.config(BusinessDataSource, {})));
