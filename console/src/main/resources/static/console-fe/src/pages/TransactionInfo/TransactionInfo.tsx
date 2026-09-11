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
import { ConfigProvider, Table, Button, DatePicker, Form, Icon, Switch, Pagination, Dialog, Input, Select } from '@alicloud/console-components';
import Actions, { LinkButton } from '@alicloud/console-components-actions';
import { withRouter } from 'react-router-dom';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import styled, { css } from 'styled-components';
import getData, { GlobalSessionParam } from '@/service/transactionInfo';
import PropTypes from 'prop-types';
import moment from 'moment';

import './index.scss';

const { RangePicker } = DatePicker;
const FormItem = Form.Item;

type StatusType = {
  label: string,
  value: number,
  iconType: string,
  iconColor: string,
}

type TransactionInfoState = {
  list: Array<any>;
  total: number;
  loading: boolean;
  branchSessionDialogVisible: boolean;
  currentBranchSession: Array<any>;
  globalSessionParam : GlobalSessionParam;
}

const statusList:Array<StatusType> = [
  {
    label: 'AsyncCommitting',
    value: 8,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'Begin',
    value: 1,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'Committing',
    value: 2,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'CommitRetrying',
    value: 3,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'Committed',
    value: 9,
    iconType: 'success',
    iconColor: '#1DC11D',
  },
  {
    label: 'CommitFailed',
    value: 10,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'CommitRetryTimeout',
    value: 16,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'Finished',
    value: 15,
    iconType: 'success',
    iconColor: '#1DC11D',
  },
  {
    label: 'Rollbacking',
    value: 4,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'RollbackRetrying',
    value: 5,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'Rollbacked',
    value: 11,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'RollbackFailed',
    value: 12,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'RollbackRetryTimeout',
    value: 17,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'TimeoutRollbacking',
    value: 6,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'TimeoutRollbackRetrying',
    value: 7,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'TimeoutRollbacked',
    value: 13,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'TimeoutRollbackFailed',
    value: 14,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'UnKnown',
    value: 0,
    iconType: 'warning',
    iconColor: '#FFA003',
  },
];

const branchSessionStatusList:Array<StatusType> = [
  {
    label: 'UnKnown',
    value: 0,
    iconType: 'warning',
    iconColor: '#FFA003',
  },
  {
    label: 'Registered',
    value: 1,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'PhaseOne_Done',
    value: 2,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'PhaseOne_Failed',
    value: 3,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'PhaseOne_Timeout',
    value: 4,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'PhaseTwo_Committed',
    value: 5,
    iconType: 'success',
    iconColor: '#1DC11D',
  },
  {
    label: 'PhaseTwo_CommitFailed_Retryable',
    value: 6,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'PhaseTwo_CommitFailed_Unretryable',
    value: 7,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'PhaseTwo_Rollbacked',
    value: 8,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'PhaseTwo_RollbackFailed_Retryable',
    value: 9,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'PhaseTwo_RollbackFailed_Unretryable',
    value: 10,
    iconType: 'error',
    iconColor: '#FF3333',
  },
];

class TransactionInfo extends React.Component<GlobalProps, TransactionInfoState> {
  static displayName = 'TransactionInfo';

  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
  };

  state: TransactionInfoState = {
    list: [],
    total: 0,
    loading: false,
    branchSessionDialogVisible: false,
    currentBranchSession: [],
    globalSessionParam: {
      withBranch: false,
      pageSize: 10,
      pageNum: 1,
    },
  };

  componentDidMount = () => {
    // search once by default
    this.search();
  }

  resetSearchFilter = () => {
    this.setState({
      globalSessionParam: {
        withBranch: false,
        // pagination info don`t reset
        pageSize: this.state.globalSessionParam.pageSize,
        pageNum: this.state.globalSessionParam.pageNum,
      },
    });
  }

  search = () => {
    this.setState({ loading: true });
    getData(this.state.globalSessionParam).then(data => {
      // if the result set is empty, set the page number to go back to the first page
      if (data.total === 0) {
        this.setState({
          list: [],
          total: 0,
          loading: false,
          globalSessionParam: Object.assign(this.state.globalSessionParam,
            { pageNum: 1 }),
        });
        return;
      }
      // format time
      data.data.forEach((element: any) => {
        element.beginTime = (element.beginTime == null || element.beginTime === '') ? null : moment(Number(element.beginTime)).format('YYYY-MM-DD HH:mm:ss');
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

  searchFilterOnChange = (key:string, val:string) => {
      this.setState({
        globalSessionParam: Object.assign(this.state.globalSessionParam,
          { [key]: val }),
      });
  }

  branchSessionSwitchOnChange = (checked: boolean, e: any) => {
    this.setState({
      globalSessionParam: Object.assign(this.state.globalSessionParam,
        { withBranch: checked }),
    });
    if (checked) {
      // if checked, do search for load branch sessions
      this.search();
    }
  }

  createTimeOnChange = (value: Array<any>) => {
    // timestamp(milliseconds)
    const timeStart = value[0] == null ? null : moment(value[0]).unix() * 1000;
    const timeEnd = value[1] == null ? null : moment(value[1]).unix() * 1000;
    this.setState({
      globalSessionParam: Object.assign(this.state.globalSessionParam,
        { timeStart, timeEnd }),
    });
  }

  statusCell = (val: number, index: number, record: any) => {
    let icon;
    statusList.forEach((status: StatusType) => {
      if (status.value === val) {
        icon = (
          <span><Icon type={status.iconType} style={{ color: status.iconColor, marginRight: '10px' }} />{status.label}</span>
        );
      }
    });
    // Unmatched
    if (icon === undefined) {
      icon = (<span>{val}</span>);
    }
    return icon;
  }

  branchSessionStatusCell = (val: number, index: number, record: any) => {
    let icon;
    branchSessionStatusList.forEach((status: StatusType) => {
      if (status.value === val) {
        icon = (
          <span><Icon type={status.iconType} style={{ color: status.iconColor, marginRight: '10px' }} />{status.label}</span>
        );
      }
    });
    // Unmatched
    if (icon === undefined) {
      icon = (<span>{val}</span>);
    }
    return icon;
  }

  operateCell = (val: string, index: number, record: any) => {
    const { locale = {}, history } = this.props;
    const {
      showBranchSessionTitle,
      showGlobalLockTitle,
    } = locale;
    return (
      <Actions style={{ width: '200px' }}>
        {/* {when withBranch false, hide 'View branch session' button} */}
        {this.state.globalSessionParam.withBranch ? (
          <LinkButton
            onClick={this.showBranchSessionDialog(val, index, record)}
          >
          {showBranchSessionTitle}
          </LinkButton>
        ) : null}

        <LinkButton
          onClick={() => {
            history.push({
              pathname: '/globallock/list',
              // @ts-ignore
              query: { xid: record.xid },
            });
          }}
        >
          {showGlobalLockTitle}
        </LinkButton>
      </Actions>);
  }

  branchSessionDialogOperateCell = (val: string, index: number, record: any) => {
    const { locale = {}, history } = this.props;
    const {
      showGlobalLockTitle,
    } = locale;
    return (
      <Actions style={{ width: '80px' }}>
        <LinkButton
          onClick={() => {
            history.push({
              pathname: '/globallock/list',
              // @ts-ignore
              query: { xid: record.xid },
            });
          }}
        >
          {showGlobalLockTitle}
        </LinkButton>
      </Actions>);
  }

  paginationOnChange = (current: number, e: {}) => {
    this.setState({
      globalSessionParam: Object.assign(this.state.globalSessionParam,
        { pageNum: current }),
    });
    this.search();
  }

  paginationOnPageSizeChange = (pageSize: number) => {
    this.setState({
      globalSessionParam: Object.assign(this.state.globalSessionParam,
        { pageSize }),
    });
    this.search();
  }

  showBranchSessionDialog = (val: string, index: number, record: any) => () => {
      this.setState({
        branchSessionDialogVisible: true,
        currentBranchSession: record.branchSessionVOs,
      });
  }

  closeBranchSessionDialog = () => {
    this.setState({
      branchSessionDialogVisible: false,
      currentBranchSession: [],
    });
  }

  render() {
    const { locale = {} } = this.props;
    const { title, subTitle, createTimeLabel,
      selectFilerPlaceholder,
      inputFilterPlaceholder,
      branchSessionSwitchLabel,
      resetButtonLabel,
      searchButtonLabel,
      operateTitle,
      branchSessionDialogTitle,
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
              onChange={(value: string) => { this.searchFilterOnChange('xid', value); }}
            />
          </FormItem>
          <FormItem name="applicationId" label="applicationId">
            <Input
              placeholder={inputFilterPlaceholder}
              onChange={(value: string) => { this.searchFilterOnChange('applicationId', value); }}
            />
          </FormItem>
          <FormItem name="status" label="status">
            <Select
              hasClear
              placeholder={selectFilerPlaceholder}
              onChange={(value: string) => { this.searchFilterOnChange('status', value); }}
              dataSource={statusList}
            />
          </FormItem>

          {/* {branch session switch} */}
          <FormItem name="withBranch" label={branchSessionSwitchLabel}>
            <Switch
              onChange={this.branchSessionSwitchOnChange}
              checked={this.state.globalSessionParam.withBranch}
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
        {/* global session table */}
        <div>
        <Table dataSource={this.state.list} loading={this.state.loading}>
          <Table.Column title="xid" dataIndex="xid" />
          <Table.Column title="transactionId" dataIndex="transactionId" />
          <Table.Column title="applicationId" dataIndex="applicationId" />
          <Table.Column title="transactionServiceGroup" dataIndex="transactionServiceGroup" />
          <Table.Column title="transactionName" dataIndex="transactionName" />
          <Table.Column
            title="status"
            dataIndex="status"
            cell={this.statusCell}
          />
          <Table.Column title="timeout" dataIndex="timeout" />
          <Table.Column title="beginTime" dataIndex="beginTime" />
          <Table.Column title="applicationData" dataIndex="applicationData" />
          <Table.Column
            title={operateTitle}
            cell={this.operateCell}
          />
        </Table>
        <Pagination
          total={this.state.total}
          defaultCurrent={1}
          current={this.state.globalSessionParam.pageNum}
          onChange={this.paginationOnChange}
          pageSize={this.state.globalSessionParam.pageSize}
          pageSizeSelector="dropdown"
          pageSizeList={[10, 20, 30, 40, 50]}
          onPageSizeChange={this.paginationOnPageSizeChange}
        />
        </div>

        {/* branch session dialog */}
        <Dialog visible={this.state.branchSessionDialogVisible} title={branchSessionDialogTitle} footer={false} onClose={this.closeBranchSessionDialog} style={{ overflowX: 'auto' }}>
          <Table dataSource={this.state.currentBranchSession}>
            <Table.Column title="transactionId" dataIndex="transactionId" />
            <Table.Column title="branchId" dataIndex="branchId" />
            <Table.Column title="resourceGroupId" dataIndex="resourceGroupId" />
            <Table.Column title="branchType" dataIndex="branchType" />
            <Table.Column
              title="status"
              dataIndex="status"
              cell={this.branchSessionStatusCell}
            />
            <Table.Column title="resourceId" dataIndex="resourceId" />
            <Table.Column title="clientId" dataIndex="clientId" />
            <Table.Column title="applicationData" dataIndex="applicationData" />
            <Table.Column
              title={operateTitle}
              cell={this.branchSessionDialogOperateCell}
            />
          </Table>
        </Dialog>
      </Page>
    );
  }
}

export default withRouter(ConfigProvider.config(TransactionInfo, {}));
