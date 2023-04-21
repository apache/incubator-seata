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
import { ConfigProvider, Table, Button, DatePicker, Form, Icon, Pagination, Input } from '@alicloud/console-components';
import Actions, { LinkButton } from '@alicloud/console-components-actions';
import { withRouter } from 'react-router-dom';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import styled, { css } from 'styled-components';
import getData, { GlobalLockParam } from '@/service/globalLockInfo';
import PropTypes from 'prop-types';
import moment from 'moment';

import './index.scss';

const { RangePicker } = DatePicker;
const FormItem = Form.Item;

type GlobalLockInfoState = {
  list: Array<any>;
  total: number;
  loading: boolean;
  globalLockParam: GlobalLockParam;
}

 class GlobalLockInfo extends React.Component<GlobalProps, GlobalLockInfoState> {
  static displayName = 'GlobalLockInfo';

  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
  };

  state: GlobalLockInfoState = {
    list: [],
    total: 0,
    loading: false,
    globalLockParam: {
      pageSize: 10,
      pageNum: 1,
    },
  }

  componentDidMount = () => {
    // @ts-ignore
    const { query } = this.props.history.location;
    if (query !== undefined) {
      const { xid } = query;
      if (xid !== undefined) {
        this.setState({
          globalLockParam: {
            xid,
            pageSize: 10,
            pageNum: 1,
          },
        }, () => this.search());
        return;
      }
    }
    // search once by default anyway
    this.search();
  }

  resetSearchFilter = () => {
    this.setState({
      globalLockParam: {
        // pagination info don`t reset
        pageSize: this.state.globalLockParam.pageSize,
        pageNum: this.state.globalLockParam.pageNum,
      },
    });
  }

  search = () => {
    this.setState({ loading: true });
    getData(this.state.globalLockParam).then(data => {
      // if the result set is empty, set the page number to go back to the first page
      if (data.total === 0) {
        this.setState({
          list: [],
          total: 0,
          loading: false,
          globalLockParam: Object.assign(this.state.globalLockParam,
            { pageNum: 1 }),
        });
        return;
      }
      // format time
      data.data.forEach((element: any) => {
        element.gmtCreate = (element.gmtCreate == null || element.gmtCreate === '') ? null : moment(Number(element.gmtCreate)).format('YYYY-MM-DD HH:mm:ss');
        element.gmtModified = (element.gmtModified == null || element.gmtModified === '') ? null : moment(Number(element.gmtModified)).format('YYYY-MM-DD HH:mm:ss');
      });

      this.setState({
        list: data.data,
        total: data.total,
        loading: false,
      });
    }).catch(err => {
      this.setState({ loading: false });
    });
  }

  createTimeOnChange = (value: Array<any>) => {
    // timestamp(milliseconds)
    const timeStart = value[0] == null ? null : moment(value[0]).unix() * 1000;
    const timeEnd = value[1] == null ? null : moment(value[1]).unix() * 1000;
    this.setState({
      globalLockParam: Object.assign(this.state.globalLockParam,
        { timeStart, timeEnd }),
    });
  }

  searchFilterOnChange = (key:string, val:string) => {
    this.setState({
      globalLockParam: Object.assign(this.state.globalLockParam,
        { [key]: val }),
    });
  }

  paginationOnChange = (current: number, e: {}) => {
    this.setState({
      globalLockParam: Object.assign(this.state.globalLockParam,
        { pageNum: current }),
    });
    this.search();
  }

  paginationOnPageSizeChange = (pageSize: number) => {
    this.setState({
      globalLockParam: Object.assign(this.state.globalLockParam,
        { pageSize }),
    });
    this.search();
  }

  render() {
    const { locale = {} } = this.props;
    const { title, subTitle, createTimeLabel,
      inputFilterPlaceholder,
      searchButtonLabel,
      resetButtonLabel,
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
        {/* search form */}
        <Form inline labelAlign="left">
          {/* {create time picker} */}
          <FormItem name="createTime" label={createTimeLabel}>
            <RangePicker
              onChange={this.createTimeOnChange}
              onOk={this.createTimeOnChange}
              showTime
              format="YYYY-MM-DD"
            />
          </FormItem>
          {/* {search filters} */}
          <FormItem name="xid" label="xid">
            <Input
              placeholder={inputFilterPlaceholder}
              value={this.state.globalLockParam.xid}
              onChange={(value: string) => { this.searchFilterOnChange('xid', value); }}
            />
          </FormItem>
          <FormItem name="tableName" label="tableName">
            <Input
              placeholder={inputFilterPlaceholder}
              onChange={(value: string) => { this.searchFilterOnChange('tableName', value); }}
            />
          </FormItem>
          <FormItem name="transactionId" label="transactionId">
            <Input
              placeholder={inputFilterPlaceholder}
              onChange={(value: string) => { this.searchFilterOnChange('transactionId', value); }}
            />
          </FormItem>
          <FormItem name="branchId" label="branchId">
            <Input
              placeholder={inputFilterPlaceholder}
              onChange={(value: string) => { this.searchFilterOnChange('branchId', value); }}
            />
          </FormItem>

          {/* {reset search filter button} */}
          <FormItem>
            <Form.Reset onClick={this.resetSearchFilter}>
              <Icon type="redo" />{resetButtonLabel}
            </Form.Reset>
          </FormItem>
          {/* {search button} */}
          <FormItem>
            <Form.Submit onClick={this.search}>
              <Icon type="search" />{searchButtonLabel}
            </Form.Submit>
          </FormItem>
        </Form>
        {/* global lock table */}
        <div>
        <Table dataSource={this.state.list} loading={this.state.loading}>
          <Table.Column title="xid" dataIndex="xid" />
          <Table.Column title="transactionId" dataIndex="transactionId" />
          <Table.Column title="branchId" dataIndex="branchId" />
          <Table.Column title="resourceId" dataIndex="resourceId" />
          <Table.Column title="tableName" dataIndex="tableName" />
          <Table.Column title="pk" dataIndex="pk" />
          <Table.Column title="rowKey" dataIndex="rowKey" />
          <Table.Column title="gmtCreate" dataIndex="gmtCreate" />
          <Table.Column title="gmtModified" dataIndex="gmtModified" />
        </Table>
        <Pagination
          total={this.state.total}
          defaultCurrent={1}
          current={this.state.globalLockParam.pageNum}
          onChange={this.paginationOnChange}
          pageSize={this.state.globalLockParam.pageSize}
          pageSizeSelector="dropdown"
          pageSizeList={[10, 20, 30, 40, 50]}
          onPageSizeChange={this.paginationOnPageSizeChange}
        />
        </div>
      </Page>
    );
  }
}

export default withRouter(ConfigProvider.config(GlobalLockInfo, {}));

