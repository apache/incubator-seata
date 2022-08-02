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
import getData from '@/service/configurationInfo';
import PropTypes from 'prop-types';

import './index.scss';
import {AppPropsType} from "@/app";


type GlobalConfigurationInfoState = {
  list: Array<any>;
  total: number;
  loading: boolean;
}

class ConfigurationInfo extends React.Component<GlobalProps, GlobalConfigurationInfoState> {
  static displayName = 'ConfigurationInfo';
  constructor(props: AppPropsType) {
    super(props);
  }
  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
    location: PropTypes.object,
  };


  state: GlobalConfigurationInfoState = {
    list: [],
    total: 0,
    loading: true,
  };
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
        });
        return;
      }
      // format time
      this.setState({
        list: data.data,
        total: data.total,
        loading: false,
      });
    }).catch(err => {
      this.setState({ loading: false });
    });
  }

  edit() {

  }

  save() {

  }
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
          <Button type="primary" className="ml-8" onClick={this.edit}>编辑</Button>
          <Button type="primary" className="ml-8" onClick={this.save}>保存</Button>
        </div>
        <Table className="mt-16" dataSource={this.state.list} loading={this.state.loading}>
          <Table.Column title="id" dataIndex="id"/>
          <Table.Column title="name" dataIndex="name"/>
          <Table.Column title="value" dataIndex="value"/>
          <Table.Column title="descr" dataIndex="descr">
          </Table.Column>

        </Table>
      </Page>
    );
  }
}

export default withRouter(ConfigProvider.config(ConfigurationInfo, {}));

