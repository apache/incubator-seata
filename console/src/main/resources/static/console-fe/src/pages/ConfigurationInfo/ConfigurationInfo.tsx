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
import { ConfigProvider, Table, Button, Dialog, DatePicker, Form, Icon, Pagination, Input } from '@alicloud/console-components';
import { withRouter } from 'react-router-dom';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import getData from '@/service/configurationInfo';
import PropTypes from 'prop-types';

import './index.scss';
import {AppPropsType} from "@/app";

const FormItem = Form.Item;
const formItemLayout = {
  labelCol: {
    fixedSpan: 5
  },
  wrapperCol: {
    span: 8
  }
}

type GlobalConfigurationInfoState = {
  list: [];
  total: number;
  loading: boolean;
  visible: boolean;
}


class ConfigurationInfo extends React.Component<GlobalProps, GlobalConfigurationInfoState> {
  static displayName = 'ConfigurationInfo';
  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
    location: PropTypes.object,
  };


  state: GlobalConfigurationInfoState = {
    list: [],
    total: 0,
    loading: true,
    visible: false,
  };
  constructor(props: AppPropsType) {
    super(props);
  }

  componentDidMount = () => {
    // search once by default
    this.search();
  }

  search = () => {
    this.setState({ loading: true });
    getData().then(data => {
      // if the result set is empty, set the page number to go back to the first page
      if (data.total === 0) {
        this.setState({
          list: [],
          total: 0,
          loading: false,
          visible: false,
        });
        return;
      }
      // format time
      this.setState({
        list: data.data,
        total: data.total,
        loading: false,
        visible: false,
      });
    }).catch(err => {
      this.setState({ loading: false });
    });
  }

  save () {

  }

  onClose = ( ) => {
    this.setState({
      visible: false
    });
  };
  onOpen = () => {
    //  console.log(item)
    this.setState({
      visible: true
    });
  };

  render() {
    const {locale = {} } = this.props;
    const {title, subTitle} = locale;
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
          <Button type="primary" className="ml-8" onClick={this.save}>保存</Button>
        </div>
        <Table className="mt-16"  dataSource={this.state.list} loading={this.state.loading}>
          <Table.Column title="id" dataIndex="id"/>
          <Table.Column title="name" dataIndex="name"/>
          <Table.Column title="value" dataIndex="value"/>
          <Table.Column title="descr" dataIndex="descr"/>
          <Table.Column title="操作"  cell={ <span> <Button type="primary"  onClick={   this.onOpen}  >编辑</Button>

          <Dialog
            title="编辑"
            visible={this.state.visible}
            onOk={this.onClose.bind(this, 'okClick')}
            onCancel={this.onClose.bind(this, 'cancelClick')}
            onClose={this.onClose}>

            <Form style={{width: '400px', height: '200px'}}  {...formItemLayout} >
              <FormItem label="配置项:">
                <h3 defaultValue={'配置项'}></h3>
              </FormItem>

              <FormItem label="配置值:">
                <Input  name="value" value={"配置值"} placeholder="请输入修改的配置值" contentEditable={true}/>
              </FormItem>

              <FormItem label="配置描述:" >
                <h3 defaultValue={"配置描述"}></h3>
              </FormItem>

            </Form>
          </Dialog>
          </span>
          }/>
        </Table>
      </Page>
    );
  }
}

export default withRouter(ConfigProvider.config(ConfigurationInfo, {}));

