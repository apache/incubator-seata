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
 import { ConfigProvider, Table, DatePicker, Form, Icon, Pagination, Input, Select, Dropdown, Menu } from '@alicloud/console-components';
 import { withRouter } from 'react-router-dom';
 import Page from '../../components/Page';
 import { GlobalProps } from '../../module';
 import PropTypes from 'prop-types';
 import moment from 'moment';
import getData, { ClientInfoParam, offline } from '../../service/clientInfo';


 const FormItem = Form.Item;

 type ClientInfoItem = {
  clientId: string,
  applicationId: string,
  resourceId: string,
  clientRole: string,
  transactionServiceGroup: string,
  resourceSets: Array<string>,
  operator: string,
 }

 type ClientInfoState = {
   list: Array<ClientInfoItem>;
   total: number;
   loading: boolean;
   currentBranchSession: Array<any>;
   clientInfoParam: ClientInfoParam;
 }

 interface ResourceSetItemProps {
  data: string[]
 }

 const ResourceSetCell = (props:ResourceSetItemProps) => (
  <Dropdown
    trigger={<div style={{ textOverflow: 'ellipsis' }}>{props.data?.join(',')}</div>}
  >
    <Menu>
      {props.data?.map(item => <Menu.Item key={item}>{item}</Menu.Item>)}
    </Menu>
  </Dropdown>
 );

 class ClientInfo extends React.Component<GlobalProps, ClientInfoState> {
   static displayName = 'ClientInfo';

   static propTypes = {
    locale: PropTypes.object,
   };

   state: ClientInfoState = {
     list: [],
     total: 0,
     loading: false,
     currentBranchSession: [],
     clientInfoParam: {
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
       clientInfoParam: {
         // pagination info don`t reset
         pageSize: this.state.clientInfoParam.pageSize,
         pageNum: this.state.clientInfoParam.pageNum,
       },
     });
   }

   search = () => {
     this.setState({ loading: true });
     getData(this.state.clientInfoParam).then(data => {
       // if the result set is empty, set the page number to go back to the first page
       if (data.total === 0) {
         this.setState({
           list: [],
           total: 0,
           loading: false,
           clientInfoParam: Object.assign(this.state.clientInfoParam,
             { pageNum: 1 }),
         });
         return;
       }
       // format time
       data.data.forEach((item: any) => {
        item.ip = item.clientId.split(':')[1];
        item.port = item.clientId.split(':')[2];
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
         clientInfoParam: Object.assign(this.state.clientInfoParam,
           { [key]: val }),
       });
   }


   createTimeOnChange = (value: Array<any>) => {
     // timestamp(milliseconds)
     const timeStart = value[0] == null ? null : moment(value[0]).unix() * 1000;
     const timeEnd = value[1] == null ? null : moment(value[1]).unix() * 1000;
     this.setState({
       clientInfoParam: Object.assign(this.state.clientInfoParam,
         { timeStart, timeEnd }),
     });
   }

   paginationOnChange = (current: number, e: {}) => {
     this.setState({
       clientInfoParam: Object.assign(this.state.clientInfoParam,
         { pageNum: current }),
     });
     this.search();
   }

   paginationOnPageSizeChange = (pageSize: number) => {
     this.setState({
       clientInfoParam: Object.assign(this.state.clientInfoParam,
        { pageSize }),
     });
     this.search();
   }

    operatorOnclick = async (record:any) => {
      const res = await offline({
        clientId: record.clientId,
        resourceId: record.resourceId,
        clientRole: record.clientRole,
      });

      if (res.code === '200') this.search();
    }

   render() {
     const { locale = {} } = this.props;
     const { title, subTitle,
       inputFilterPlaceholder,
       resetButtonLabel,
       searchButtonLabel,
       operateTitle,
       offlineTitle,
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
           {/* {search filters} */}
           <FormItem name="resourceId" label="resourceId">
             <Input
               placeholder={inputFilterPlaceholder}
               onChange={(value: string) => { this.searchFilterOnChange('resourceId', value); }}
             />
           </FormItem>
           <FormItem name="applicationId" label="applicationId">
             <Input
               placeholder={inputFilterPlaceholder}
               onChange={(value: string) => { this.searchFilterOnChange('applicationId', value); }}
             />
           </FormItem>
           <FormItem name="ip" label="ip">
             <Input
               placeholder={inputFilterPlaceholder}
               onChange={(value: string) => { this.searchFilterOnChange('ip', value); }}
             />
           </FormItem>
           <FormItem name="clientRole" label="clientRole">
             <Select
               dataSource={[{ value: 'TMROLE', label: 'TMROLE' }, { value: 'RMROLE', label: 'RMROLE' }]}
               placeholder={inputFilterPlaceholder}
               onChange={(value: string) => { this.searchFilterOnChange('clientRole', value); }}
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
           <Table.Column title="clientId" dataIndex="clientId" />
           <Table.Column title="applicationId" dataIndex="applicationId" />
           <Table.Column title="resourceId" dataIndex="resourceId" />
           <Table.Column title="ip" dataIndex="ip" />
           <Table.Column title="port" dataIndex="port" />
           <Table.Column title="clientRole" dataIndex="clientRole" />
           <Table.Column title="transactionServiceGroup" dataIndex="transactionServiceGroup" />
           <Table.Column title="resourceSets" dataIndex="resourceSets" cell={(value:string[]) => <ResourceSetCell data={value} />} />
           <Table.Column title={operateTitle} dataIndex="operator" cell={(a, b, record:any) => <a onClick={() => this.operatorOnclick(record)}>{offlineTitle}</a>}/>
         </Table>
         <Pagination
           total={this.state.total}
           defaultCurrent={1}
           current={this.state.clientInfoParam.pageNum}
           onChange={this.paginationOnChange}
           pageSize={this.state.clientInfoParam.pageSize}
           pageSizeSelector="dropdown"
           pageSizeList={[10, 20, 30, 40, 50]}
           onPageSizeChange={this.paginationOnPageSizeChange}
         />
         </div>
       </Page>
     );
   }
 }

 export default withRouter(ConfigProvider.config(ClientInfo, {}));
