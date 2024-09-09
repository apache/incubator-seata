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
import {
  ConfigProvider,
  Table,
  Button,
  DatePicker,
  Form,
  Icon,
  Pagination,
  Input,
  Dialog,
  Message,
  Select,
  Upload,
} from '@alicloud/console-components';
import { withRouter } from 'react-router-dom';

import {getConfig, getClusterInfo, putConfig, deleteConfig, deleteAllConfig, getAllNamespaces, getAllDataIds, uploadConfig} from "@/service/configInfo";
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import './index.scss';

import moment from "moment/moment";
type ConfigInfoState = {
  configList: Array<any>;
  editDialogVisible: boolean;
  deleteDialogVisible: boolean;
  uploadDialogVisible: boolean;
  loading: boolean;
  configParam: ConfigParam;
  editDialogInfo: DialogInfo;
  deleteDialogInfo: DeleteDialogInfo;
  uploadDialogInfo: UploadDialogInfo;
  isRaft: boolean;
  namespaces: Array<string>;
  dataIds: Array<string>;
}
export type ConfigParam = {
  namespace: string,
  dataId: string,
};

type DialogInfo = {
  isEdit: boolean;
  namespace: string;
  dataId: string;
  key: string;
  value: string;
}

type DeleteDialogInfo = {
  namespace: string;
  dataId: string;
}

type UploadDialogInfo = {
  namespace: string;
  dataId: string;
  file: File;
}

const FormItem = Form.Item;



class ConfigInfo extends React.Component<GlobalProps, ConfigInfoState> {
  static displayName = 'ConfigInfo';

  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
  };

  state: ConfigInfoState = {
    configList: [],
    loading: false,
    editDialogVisible: false,
    deleteDialogVisible: false,
    uploadDialogVisible: false,
    namespaces: [],
    dataIds: [],
    configParam: {
      namespace: '',
      dataId: '',
    },
    editDialogInfo: {
      isEdit: false,
      namespace: '',
      dataId: '',
      key: '',
      value: '',
    },
    deleteDialogInfo: {
      namespace: '',
      dataId: '',
    },
    uploadDialogInfo: {
      namespace: '',
      dataId: '',
      file: null,
    },
    isRaft: false,
  }

  componentDidMount() {
    this.init();
    this.pollingInterval = setInterval(this.refreshConfigData, 10000); // 每10秒刷新一次
  }
  componentWillUnmount() {
    clearInterval(this.pollingInterval);
  }

  init = async () => {
    const { disableTitle } = this.props.locale;
    this.setState({ loading: true });
    try {
      const response = await getClusterInfo();
      const raftMode = response.configMode
      if (raftMode === 'raft') {
        this.setState({ isRaft: true, loading: false });
        this.fetchNamespaces();
      }else{
        this.setState({ loading: false });
        Message.error(disableTitle);
        setTimeout(() => this.props.history.goBack(), 1000);
      }
      //this.setState({ clusterInfo: result, loading: false });
    } catch (error) {
      Message.error('Failed to fetch cluster info');
      this.setState({ loading: false });
      this.props.history.goBack();
    }
  }

  refreshConfigData = () => {
    this.fetchNamespaces();
    if (this.state.configParam.namespace) {
      this.fetchDataIds(this.state.configParam.namespace);
    }
  }
  fetchNamespaces = async () => {
    try {
      const response = await getAllNamespaces();
      const result = response.result;
      this.setState({ namespaces: result });
    } catch (error) {
      Message.error('Failed to fetch namespace list');
    }
  }

  fetchDataIds = async (namespace: string) => {
    try {
      const response = await getAllDataIds({ namespace });
      const result = response.result;
      this.setState({ dataIds: result });
    } catch (error) {
      Message.error('Failed to fetch dataIds');
    }
  }

  fetchConfigList = async () => {
    this.setState({ loading: true });
    try {
      const response = await getConfig({namespace: this.state.configParam.namespace, dataId: this.state.configParam.dataId});
      if (response.success && response.result){
        const { config } = response.result;
        console.log(config);
        const configList = Object.keys(config).map((key) => ({ ...config[key] }));
        this.setState({ configList, loading: false });
      }else {
        Message.error(response.errMsg || 'Failed to fetch config list');
        this.setState({ loading: false });
      }
    } catch (error) {
      Message.error('Failed to fetch config list');
      this.setState({ loading: false });
    }
  }
  searchFilterOnChange = async (key:string, val:string) => {
    this.setState({
      configParam: Object.assign(this.state.configParam,
        { [key]: val }),
    });
    if (key === 'namespace') {
      this.setState({
        configParam: Object.assign(this.state.configParam,
          { dataId: '' }),
      });
      await this.fetchDataIds(val);
    }
  }
  search = () => {
    this.fetchConfigList();
  }
  resetSearchFilter = () => {
    this.setState({
      configParam: {
        // pagination info don`t reset
        namespace: '',
        dataId: '',
      },
    });
  }

  resetDialog = () => {
    this.setState({
      editDialogInfo: {isEdit: true, namespace: this.state.configParam.namespace, dataId: this.state.configParam.dataId, key: '', value: ''},
      deleteDialogInfo: {namespace: '', dataId: ''},
      uploadDialogInfo: {namespace: '', dataId: '', file: null},
    });
  };
  openEditDialog = (config: { key: string; value: string }) => {
    this.setState({ editDialogVisible: true, editDialogInfo: {isEdit: true, namespace: this.state.configParam.namespace, dataId: this.state.configParam.dataId, ...config}});
  };

  openDeleteDialog = () => {
    this.setState({ deleteDialogVisible: true, deleteDialogInfo:  {namespace: this.state.configParam.namespace, dataId: this.state.configParam.dataId}});
  }

  openUploadDialog = () => {
    this.setState({ uploadDialogVisible: true, uploadDialogInfo:  {namespace: this.state.configParam.namespace, dataId: '', file: null}});
  }

  createConfig = () => {
    this.setState({ editDialogVisible: true, editDialogInfo: {isEdit: false, namespace: this.state.configParam.namespace, dataId: this.state.configParam.dataId, key: '', value: ''}});
  };

  closeDialog = () => {
    this.setState({ editDialogVisible: false, deleteDialogVisible: false, uploadDialogVisible: false});
    this.resetDialog();
  };

  handleAddOrEditConfig = async () => {
    const { operationSuccess, operationFail } = this.props.locale;
    const { editDialogInfo } = this.state;
    try {
      const response =await putConfig({
        namespace: editDialogInfo.namespace,
        dataId: editDialogInfo.dataId,
        key: editDialogInfo.key,
        value: editDialogInfo.value,
      });
      if (response.success) {
        Message.success(operationSuccess);
        this.setState({
          editDialogVisible: false,
          configParam: {
            namespace: editDialogInfo.namespace,
            dataId: editDialogInfo.dataId,
          }
        });
        this.fetchNamespaces();
        this.fetchDataIds(editDialogInfo.namespace);
        this.fetchConfigList();
      } else {
          Message.error(response.errMsg || operationFail);
        }
    } catch (error) {
      Message.error(operationFail);
    }
  }

  handleDeleteConfig = async (record: { key: string }) => {
    const { deleteTitle, deleteConfirmLabel, operationSuccess, operationFail } = this.props.locale;
    Dialog.confirm({
      title: deleteTitle,
      content: deleteConfirmLabel + `${record.key} ?`,
      onOk: async () => {
        try {
          const response = await deleteConfig({ namespace: this.state.configParam.namespace, dataId: this.state.configParam.dataId, key: record.key });
          if (response.success) {
            Message.success(operationSuccess);
            this.fetchConfigList();
          }else {
            Message.error(response.errMsg || operationFail);
          }
        } catch (error) {
          Message.error(operationFail);
        }
      },
      onCancel: () => {

      },
    });
  }

  handleDeleteAllConfig = async () => {
    const {operationSuccess, operationFail, fieldFillingTips} = this.props.locale;
    const { namespace, dataId } = this.state.deleteDialogInfo;

    if (!namespace || !dataId) {
      Message.error(fieldFillingTips);
      return;
    }
    try {
      const response = await deleteAllConfig({ namespace, dataId });
      if (response.success) {
        Message.success(operationSuccess);
        this.setState({
          configParam: {
            namespace: namespace,
            dataId: dataId,
          },
          deleteDialogVisible: false,
          deleteDialogInfo: { namespace: '', dataId: '' },
        });
        this.fetchDataIds(namespace)
        this.fetchConfigList();
      } else {
        Message.error(response.errMsg || operationFail);
      }
    } catch (error) {
      Message.error(operationFail);
    }
  }

  handleUploadConfig = async () => {
    const {operationSuccess, operationFail, fieldFillingTips} = this.props.locale;
    const { namespace, dataId,file } = this.state.uploadDialogInfo;
    if (!namespace || !dataId || !file) {
      Message.error(fieldFillingTips);
      return;
    }
    const formData = new FormData();
    formData.append('namespace', namespace);
    formData.append('dataId', dataId);
    formData.append('file', file);

    try {
      const response = await uploadConfig(formData);
      if (response.success) {
        Message.success(operationSuccess);
        this.setState({
          uploadDialogVisible: false,
          uploadDialogInfo: { namespace: '', dataId: '', file: null},
          configParam: {
            namespace: namespace,
            dataId: dataId,
          }
        });
        this.fetchNamespaces();
        this.fetchDataIds(namespace)
        this.fetchConfigList();
      } else {
        Message.error(response.errMsg || operationFail);
      }
    } catch (error) {
      Message.error(operationFail);
    }
  }

  handleDialogInputChange = (key: string, value: string) => {
    this.setState((prevState) => ({
      editDialogInfo: {
        ...prevState.editDialogInfo,
        [key]: value,
      },
    }));
  };

  handleDeleteDialogInputChange = (key: string, value: string) => {
    this.setState((prevState) => ({
      deleteDialogInfo: {
        ...prevState.deleteDialogInfo,
        [key]: value
      }
    }))
   }

  handleUploadDialogInputChange = (key: string, value: string) => {
    this.setState((prevState) => ({
      uploadDialogInfo: {
        ...prevState.uploadDialogInfo,
        [key]: value
      }
    }))
  }

  handleFileInputChange = (fileList: Array<any>) => {
    const file = fileList.length > 0 ? fileList[0] : null;
    if (file && file.originFileObj) {
      this.handleUploadDialogInputChange('file', file.originFileObj);
    }
  }
  render() {
    const { locale = {} } = this.props;
    const { title, subTitle,
      searchButtonLabel,
      resetButtonLabel,
      createButtonLabel,
      clearButtonLabel,
      uploadButtonLabel,
      operateTitle,
      editTitle,
      deleteTitle,
      uploadTitle,
      deleteAllConfirmLabel,
      editButtonLabel,
      deleteButtonLabel,
      inputFilterPlaceholder,
      uploadFileButtonLabel,
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
          <FormItem name="namespace" label="namespace">
            <Select
              placeholder={inputFilterPlaceholder}
              value={this.state.configParam.namespace}
              onChange={(value: string) => { this.searchFilterOnChange('namespace', value); }}
              dataSource={this.state.namespaces}
              style={{ width: 200 }}
              hasClear={true}
            />
          </FormItem>
          <FormItem name="dataId" label={"dataId"}>
            <Select
              placeholder={inputFilterPlaceholder}
              value={this.state.configParam.dataId}
              onChange={(value: string) => { this.searchFilterOnChange('dataId', value); }}
              dataSource={this.state.dataIds}
              style={{ width: 200 }}
              hasClear={true}
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
          <FormItem>
            <Form.Submit onClick={this.createConfig}>
              <Icon type="add" />{createButtonLabel}
            </Form.Submit>
          </FormItem>
          <FormItem>
            <Form.Submit onClick={this.openUploadDialog}>
              <Icon type="upload" />{uploadButtonLabel}
            </Form.Submit>
          </FormItem>
          <FormItem>
            <Form.Submit onClick={this.openDeleteDialog}>
              <Icon type="delete" />{clearButtonLabel}
            </Form.Submit>
          </FormItem>
        </Form>

        {/* config info table */}
        <div>
          <Table dataSource={this.state.configList} loading={this.state.loading} tableLayout={"fixed"}>
            <Table.Column title="Key" dataIndex="key"  />
            <Table.Column title="Value" dataIndex="value"  />
            <Table.Column title="Default Value" dataIndex="defaultValue"  />
            <Table.Column title="Description" dataIndex="description" />
            <Table.Column
              title={operateTitle}
              cell={(value, index, record) => (
                <>
                  <Button
                    type="primary"
                    onClick={() => this.openEditDialog(record)}
                    style={{ marginRight: 8 }}
                  >
                    <Icon type="edit" />{editButtonLabel}
                  </Button>
                  <Button
                    type="normal"
                    onClick={() => this.handleDeleteConfig(record)}
                    warning
                  >
                    <Icon type="ashbin" />{deleteButtonLabel}
                  </Button>
                </>
              )}
            />
          </Table>
        </div>
        {/* config edit dialog */}
        <Dialog
          title={editTitle}
          visible={this.state.editDialogVisible}
          onOk={this.handleAddOrEditConfig}
          onCancel={this.closeDialog}
          onClose={this.closeDialog}
          style={{ width: '600px' }}
        >
          <Form labelAlign="top">
            <FormItem label="Namespace">
              <Input
                disabled={this.state.editDialogInfo.isEdit}
                value={this.state.editDialogInfo.namespace}
                onChange={(value: string) => this.handleDialogInputChange('namespace', value)}
              />
            </FormItem>
            <FormItem label="Data ID">
              <Input
                disabled={this.state.editDialogInfo.isEdit}
                value={this.state.editDialogInfo.dataId}
                onChange={(value: string) => this.handleDialogInputChange('dataId', value)}
              />
            </FormItem>
            <FormItem label="Key">
              <Input
                disabled={this.state.editDialogInfo.isEdit}
                value={this.state.editDialogInfo.key}
                onChange={(value: string) => this.handleDialogInputChange('key', value)}
              />
            </FormItem>
            <FormItem label="Value">
              <Input
                value={this.state.editDialogInfo.value}
                onChange={(value: string) => this.handleDialogInputChange('value', value)}
              />
            </FormItem>
          </Form>
        </Dialog>

        {/* config delete dialog*/}
        <Dialog
          title={deleteAllConfirmLabel}
          visible={this.state.deleteDialogVisible}
          onOk={this.handleDeleteAllConfig}
          onCancel={this.closeDialog}
          onClose={this.closeDialog}
          style={{ width: '600px' }}
        >
          <Form labelAlign="top"
          >
            <FormItem label="Namespace"
                      required
                      requiredMessage="Namespace cannot be empty"
            >

              <Input
                value={this.state.deleteDialogInfo.namespace}
                onChange={(value: string) =>{
                  this.handleDeleteDialogInputChange('namespace', value)}
              }
              />
            </FormItem>
            <FormItem label="DataID"
                      required
                      requiredMessage="DataID cannot be empty"
            >
              <Input
                value={this.state.deleteDialogInfo.dataId}
                onChange={(value: string) => {
                  this.handleDeleteDialogInputChange('dataId', value)}
              }
              />
            </FormItem>
          </Form>
        </Dialog>

        {/* config upload dialog*/}
        <Dialog
          title={uploadTitle}
          visible={this.state.uploadDialogVisible}
          onOk={this.handleUploadConfig}
          onCancel={this.closeDialog}
          onClose={this.closeDialog}
          style={{ width: '600px' }}
        >
          <Form labelAlign="top"
          >
            <FormItem label="Namespace"
                      required
                      requiredMessage="Namespace cannot be empty"
            >

              <Input
                value={this.state.uploadDialogInfo.namespace}
                onChange={(value: string) => this.handleUploadDialogInputChange('namespace', value)}
              />
            </FormItem>
            <FormItem label="DataID"
                      required
                      requiredMessage="DataID cannot be empty"
            >
              <Input
                value={this.state.uploadDialogInfo.dataId}
                onChange={(value: string) => this.handleUploadDialogInputChange('dataId', value)}
              />
            </FormItem>
            <FormItem label="File (Support txt, text, yaml, properties file)" required requiredMessage="File cannot be empty">
              <Upload
                listType="text"
                onChange={this.handleFileInputChange}
                beforeUpload={() => false}  // Prevent auto-upload
                accept={".txt,.text,.yaml,.yml,.properties"}
                limit={1}
              >
                <Button>
                  <Icon type="upload" />{uploadFileButtonLabel}
                </Button>
              </Upload>
            </FormItem>
          </Form>
        </Dialog>
      </Page>
    );
  }
}
export default withRouter(ConfigProvider.config(ConfigInfo, {}));
