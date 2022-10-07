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
import putConfig from '@/service/configurationPut';
import PropTypes from 'prop-types';

import './index.scss';
import {AppPropsType} from "@/app";
import Actions, {LinkButton} from "@alicloud/console-components-actions";

const FormItem = Form.Item;
const formItemLayout = {
  labelCol: {
    fixedSpan: 5
  },
  wrapperCol: {
    span: 8
  }
}

type ConfigurationInfoState = {
  list: [];
  loading: boolean;
  configurationDialogVisible: boolean;
  configurationData: any;
}


class ConfigurationInfo extends React.Component<GlobalProps, ConfigurationInfoState> {
  static displayName = 'ConfigurationInfo';
  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
    location: PropTypes.object,
  };


  state: ConfigurationInfoState = {
    list: [],
    loading: true,
    configurationDialogVisible: false,
    configurationData: null,
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
      this.setState({
        list: data.data || [],
        loading: false,
        configurationDialogVisible: false,
      });
    }).catch(err => {
      this.setState({ loading: false });
    });
  }

  onSave = () => {
    if (this.state.configurationData.newValue === undefined || this.state.configurationData.newValue === this.state.configurationData.value) {
      alert("请先修改");
      return;
    }

    putConfig(this.state.configurationData.name, this.state.configurationData.newValue).then(data => {
      this.onClose();
      this.search();
    });
  }

  onClose = ( ) => {
    this.setState({
      configurationDialogVisible: false,
      configurationData: null
    });
  };
  onOpen = (val: string, index: number, record: any) => {
    const { locale = {} } = this.props;
    const { editButtonTitle } = locale;
    return (
      <Actions style={{ width: '200px' }}>
        <LinkButton
          onClick={this.showConfigurationDialog(val, index, record)}
        >
          {editButtonTitle}
        </LinkButton>
      </Actions>);
  }

  showConfigurationDialog = (val: string, index: number, record: any) => () => {
    this.setState({
      configurationDialogVisible: true,
      configurationData: record,
    });
  }

  render() {
    const {locale = {} } = this.props;
    const {title, subTitle, operateTitle, configurationDialogTitle} = locale;
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
        <Table className="mt-16" dataSource={this.state.list} loading={this.state.loading}>
          <Table.Column title="id" dataIndex="id"/>
          <Table.Column title="name" dataIndex="name"/>
          <Table.Column title="value" dataIndex="value"/>
          <Table.Column title="descr" dataIndex="descr"/>
          <Table.Column
            title={operateTitle}
            cell={this.onOpen}
          />
        </Table>

        {/* Edit session dialog */}
        {
          <Dialog
            title={configurationDialogTitle}
            visible={this.state.configurationDialogVisible}
            onOk={this.onSave}
            onCancel={this.onClose.bind(this, 'cancelClick')}
            onClose={this.onClose}>
            {
              this.state.configurationData
                ?
                <Form style={{width: '400px', height: '200px'}}  {...formItemLayout} >
                  <FormItem label="配置项:">
                    <h3>{this.state.configurationData.name}</h3>
                  </FormItem>

                  <FormItem label="配置值:">
                    <Input
                      name="value"
                      defaultValue={this.state.configurationData.value}
                      onChange={(value: string) => { this.state.configurationData.newValue = value; }}
                      placeholder="请输入修改的配置值"
                      contentEditable={true}
                    />
                  </FormItem>

                  <FormItem label="配置描述:" >
                    <h3>{this.state.configurationData.descr}</h3>
                  </FormItem>
                </Form>
                :
                null
            }
          </Dialog>
        }
      </Page>
    );
  }
}

export default withRouter(ConfigProvider.config(ConfigurationInfo, {}));

