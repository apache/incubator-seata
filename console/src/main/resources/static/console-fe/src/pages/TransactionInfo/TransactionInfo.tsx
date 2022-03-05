/**
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React from 'react';
import { ConfigProvider, Table, Button, DatePicker, Form, Search, Icon, Switch } from '@alicloud/console-components';
import { withRouter } from 'react-router-dom';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import styled, { css } from 'styled-components';
import getData from '@/service/transactionInfo';
import PropTypes from 'prop-types';

import './index.scss';

const { RangePicker } = DatePicker;
const FormItem = Form.Item;

const dataSource = [
  { value: 'xid', label: 'xid' },
  { value: 'applicationId', label: 'applicationId' },
  { value: 'status', label: 'status' },
];

type TransactionInfoState = {
  list:Array<any>;
  searchFilter:Array<any>;
}

class TransactionInfo extends React.Component<GlobalProps, TransactionInfoState> {
  static displayName = 'TransactionInfo';

  static propTypes = {
    locale: PropTypes.object,
  };

  state: TransactionInfoState = {
    list: [],
    searchFilter: [
      {
        label: 'Products',
        value: 'Products',
      },
      {
        label: 'Products1',
        value: 'Products1',
      }],
  };

  componentDidMount() {
    getData().then(response => {
      this.setState({ list: response.data });
    });
  }

  onChange = () => {

  }

  render() {
    const { locale = {} } = this.props;
    const { title, subTitle, createTimeLabel,
      searchFilerPlaceholder,
      searchPlaceholder,
      branchSessionSwitchLabel,
      resetButtonLabel,
      searchButtonLabel,
    } = locale;
    return (
      <Page
        title={title}
        breadcrumbs={[
        {
          link: '/',
          text: title,
        },
        {
          text: subTitle,
        },
      ]}
      >
        <div>
          <Form inline labelAlign="left">
            <FormItem label={createTimeLabel}>
              <RangePicker onChange={this.onChange} showTime />
            </FormItem>
            <FormItem >
            <Search
              style={{ width: '400px' }}
              shape="simple"
              hasIcon={false}
              // onChange={this.onChange.bind(this)}
              // onSearch={this.onSearch.bind(this)}
              filterProps={{ placeholder: searchFilerPlaceholder }}
              placeholder={searchPlaceholder}
              filter={this.state.searchFilter}
              // onFilterChange={this.onFilterChange.bind(this)}
            />
            </FormItem>
            <FormItem><Button><Icon type="add" /></Button></FormItem>
            <FormItem label={branchSessionSwitchLabel}>
              <Switch />
            </FormItem>
            <FormItem><Button><Icon type="redo" />{resetButtonLabel}</Button></FormItem>
            <FormItem><Button><Icon type="search" />{searchButtonLabel}</Button></FormItem>
          </Form>
        </div>
        <Table className="mt-16" dataSource={this.state.list}>
          <Table.Column title="xid" dataIndex="xid" />
          <Table.Column title="transactionId" dataIndex="transactionId" />
          <Table.Column title="applicationId" dataIndex="applicationId" />
          <Table.Column title="transactionServiceGroup" dataIndex="transactionServiceGroup" />
          <Table.Column title="transactionName" dataIndex="transactionName" />
          <Table.Column title="status" dataIndex="status" />
          <Table.Column title="timeout" dataIndex="timeout" />
          <Table.Column title="beginTime" dataIndex="beginTime" />
          <Table.Column title="applicationData" dataIndex="applicationData" />
        </Table>
      </Page>
    );
  }
}

export default withRouter(ConfigProvider.config(TransactionInfo, {}));
