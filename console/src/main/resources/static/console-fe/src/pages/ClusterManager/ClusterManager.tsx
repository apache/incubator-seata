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
import { ConfigProvider, Table, Button, Form, Icon, Dialog, Select, Message } from '@alicloud/console-components';
import Actions from '@alicloud/console-components-actions';
import { withRouter } from 'react-router-dom';
import { connect } from 'react-redux';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import { fetchNamespaceV2, fetchClusterData } from '@/service/clusterManager';
import PropTypes from 'prop-types';

import './index.scss';

const FormItem = Form.Item;

type ClusterManagerLocale = {
  title?: string;
  subTitle?: string;
  selectNamespaceFilerPlaceholder?: string;
  selectClusterFilerPlaceholder?: string;
  searchButtonLabel?: string;
  unitName?: string;
  members?: string;
  clusterType?: string;
  view?: string;
  unitDialogTitle?: string;
  control?: string;
  transaction?: string;
  weight?: string;
  healthy?: string;
  term?: string;
  unit?: string;
  operations?: string;
  internal?: string;
  version?: string;
  metadata?: string;
  controlEndpoint?: string;
  transactionEndpoint?: string;
  metadataDialogTitle?: string;
  role?: string;
};

type ClusterManagerState = {
  namespaceOptions: Map<string, { clusters: string[], clusterVgroups: {[key: string]: string[]}, clusterTypes: {[key: string]: string} }>;
  clusters: Array<string>;
  namespace?: string;
  cluster?: string;
  clusterData: any; // ClusterData
  loading: boolean;
  unitDialogVisible: boolean;
  selectedUnit: any; // Unit
  selectedUnitName: string;
  metadataDialogVisible: boolean;
  selectedMetadata: any;
};

class ClusterManager extends React.Component<GlobalProps, ClusterManagerState> {
  static displayName = 'ClusterManager';

  static propTypes = {
    locale: PropTypes.object,
  };

  state: ClusterManagerState = {
    namespaceOptions: new Map<string, { clusters: string[], clusterVgroups: {[key: string]: string[]}, clusterTypes: {[key: string]: string} }>(),
    clusters: [],
    clusterData: null,
    loading: false,
    unitDialogVisible: false,
    selectedUnit: null,
    selectedUnitName: '',
    metadataDialogVisible: false,
    selectedMetadata: null,
  };

  componentDidMount = () => {
    this.loadNamespaces();
  };

  loadNamespaces = async () => {
    try {
      const namespaces = await fetchNamespaceV2();
      const namespaceOptions = new Map<string, { clusters: string[], clusterVgroups: {[key: string]: string[]}, clusterTypes: {[key: string]: string} }>();
      Object.keys(namespaces).forEach(namespaceKey => {
        const namespaceData = namespaces[namespaceKey];
        const clustersData = namespaceData.clusters || {};
        const clusterVgroups: {[key: string]: string[]} = {};
        const clusterTypes: {[key: string]: string} = {};
        Object.keys(clustersData).forEach(clusterName => {
          const cluster = clustersData[clusterName];
          clusterVgroups[clusterName] = cluster.vgroups || [];
          clusterTypes[clusterName] = cluster.type || 'default';
        });
        const clusters = Object.keys(clustersData);
        namespaceOptions.set(namespaceKey, {
          clusters,
          clusterVgroups,
          clusterTypes,
        });
      });
      if (namespaceOptions.size > 0) {
        const firstNamespace = Array.from(namespaceOptions.keys())[0];
        const selectedNamespace = namespaceOptions.get(firstNamespace);
        const firstCluster = selectedNamespace ? selectedNamespace.clusters[0] : undefined;
        this.setState(prevState => ({
          ...prevState,
          namespaceOptions,
          namespace: firstNamespace,
          cluster: firstCluster,
          clusters: selectedNamespace ? selectedNamespace.clusters : [],
        }), () => {
          this.search();
        });
      } else {
        this.setState(prevState => ({
          ...prevState,
          namespaceOptions,
        }));
      }
    } catch (error) {
      console.error('Failed to fetch namespaces:', error);
    }
  };

  searchFilterOnChange = (key: string, val: string) => {
    if (key === 'namespace') {
      const selectedNamespace = this.state.namespaceOptions.get(val);
      const clusters = selectedNamespace ? selectedNamespace.clusters : [];
      const firstCluster = clusters.length > 0 ? clusters[0] : undefined;
      this.setState(prevState => ({
        ...prevState,
        namespace: val,
        cluster: firstCluster,
        clusters,
      }));
    } else if (key === 'cluster') {
      this.setState(prevState => ({
        ...prevState,
        cluster: val,
      }));
    }
  };

  search = () => {
    const { namespace, cluster } = this.state;
    if (!namespace || !cluster) {
      Message.error('Please select namespace and cluster');
      return;
    }
    this.setState(prevState => ({
      ...prevState,
      loading: true,
    }));
    fetchClusterData(namespace, cluster).then(data => {
      if (data.success) {
        this.setState(prevState => ({
          ...prevState,
          clusterData: data.data,
          loading: false,
        }));
      } else {
        Message.error(data.message || 'Failed to fetch cluster data');
        this.setState(prevState => ({
          ...prevState,
          loading: false,
        }));
      }
    }).catch(err => {
      Message.error('Failed to fetch cluster data');
      this.setState(prevState => ({
        ...prevState,
        loading: false,
      }));
    });
  };

  showUnitDialog = (unitName: string, unit: any) => {
    this.setState(prevState => ({
      ...prevState,
      unitDialogVisible: true,
      selectedUnit: unit,
      selectedUnitName: unitName,
    }));
  };

  closeUnitDialog = () => {
    this.setState(prevState => ({
      ...prevState,
      unitDialogVisible: false,
      selectedUnit: null,
      selectedUnitName: '',
    }));
  };

  showMetadataDialog = (metadata: any) => {
    this.setState(prevState => ({
      ...prevState,
      metadataDialogVisible: true,
      selectedMetadata: metadata,
    }));
  };

  closeMetadataDialog = () => {
    this.setState(prevState => ({
      ...prevState,
      metadataDialogVisible: false,
      selectedMetadata: null,
    }));
  };

  render() {
    const { locale } = this.props;
    const rawLocale = locale.ClusterManager;
    const clusterManagerLocale: ClusterManagerLocale = typeof rawLocale === 'object' && rawLocale !== null ? rawLocale : {};
    const { title, subTitle, selectNamespaceFilerPlaceholder, selectClusterFilerPlaceholder, searchButtonLabel, members, clusterType, view, unitDialogTitle, control, transaction, weight, healthy, term, unit, operations, internal, version, metadata, controlEndpoint, transactionEndpoint, metadataDialogTitle, role } = clusterManagerLocale;
    const unitData = this.state.clusterData ? Object.entries(this.state.clusterData.unitData || {}) : [];
    const { namespace } = this.state;
    const namespaceData = namespace ? this.state.namespaceOptions.get(namespace) : null;
    return (
      <Page
        title={title || 'Cluster Manager'}
        breadcrumbs={[
          {
            link: '/',
            text: title || 'Cluster Manager',
          },
          {
            text: subTitle || 'Manage Clusters',
          },
        ]}
      >
        {/* search form */}
        <Form inline labelAlign="left">
          <FormItem name="namespace" label="namespace">
            <Select
              hasClear
              placeholder={selectNamespaceFilerPlaceholder || 'Select namespace'}
              onChange={(value: string) => {
                this.searchFilterOnChange('namespace', value);
              }}
              dataSource={Array.from(this.state.namespaceOptions.keys()).map(key => ({ label: key, value: key }))}
              value={this.state.namespace}
            />
          </FormItem>
          <FormItem name="cluster" label="cluster">
            <Select
              hasClear
              placeholder={selectClusterFilerPlaceholder || 'Select cluster'}
              onChange={(value: string) => {
                this.searchFilterOnChange('cluster', value);
              }}
              dataSource={this.state.clusters.map(value => ({ label: value, value }))}
              value={this.state.cluster}
            />
          </FormItem>
          <FormItem>
            <Form.Submit onClick={this.search}>
              <Icon type="search" />{searchButtonLabel || 'Search'}
            </Form.Submit>
          </FormItem>
        </Form>
        {/* unit table */}
        <div style={{ marginTop: '20px' }}>
          <Table dataSource={unitData} loading={this.state.loading}>
            <Table.Column title={members || 'Members'} dataIndex="1" cell={(val: any) => (val.namingInstanceList ? val.namingInstanceList.length : 0)} />
            <Table.Column title={clusterType || 'Cluster Type'} cell={() => (this.state.clusterData ? this.state.clusterData.clusterType : '')} />
            <Table.Column
              title={operations || 'Operations'}
              cell={(val: any, index: number, record: any) => {
                return (
                  <Actions>
                    <Button onClick={() => this.showUnitDialog(record[0], record[1])}>
                      {view || 'View'}
                    </Button>
                  </Actions>
                );
              }}
            />
          </Table>
        </div>

        {/* unit dialog */}
        <Dialog visible={this.state.unitDialogVisible} title={`${unitDialogTitle || 'Unit'}: ${this.state.selectedUnitName}`} footer={false} onClose={this.closeUnitDialog} style={{ width: '80vw', height: '80vh', overflow: 'auto' }}>
          <Table dataSource={this.state.selectedUnit ? this.state.selectedUnit.namingInstanceList || [] : []} style={{ overflow: 'auto' }}>
            <Table.Column title={control || 'Control'} dataIndex="control" cell={(val: any) => (val ? `${controlEndpoint || 'Control Endpoint'}: ${val.host}:${val.port}` : '')} />
            <Table.Column title={transaction || 'Transaction'} dataIndex="transaction" cell={(val: any) => (val ? `${transactionEndpoint || 'Transaction Endpoint'}: ${val.host}:${val.port}` : '')} />
            <Table.Column title={internal || 'Internal'} dataIndex="internal" cell={(val: any) => (val ? `${val.host}:${val.port}` : '')} />
            <Table.Column title={weight || 'Weight'} dataIndex="weight" />
            <Table.Column title={healthy || 'Healthy'} dataIndex="healthy" cell={(val: boolean) => (val ? 'Yes' : 'No')} />
            <Table.Column title={term || 'Term'} dataIndex="term" />
            <Table.Column title={role || 'Role'} dataIndex="role" />
            <Table.Column title={unit || 'Unit'} dataIndex="unit" />
            <Table.Column title={version || 'Version'} dataIndex="version" />
            <Table.Column title={metadata || 'Metadata'} dataIndex="metadata" cell={(val: any) => (val ? <Button onClick={() => this.showMetadataDialog(val)}>View JSON</Button> : '')} />
          </Table>
        </Dialog>

        {/* metadata dialog */}
        <Dialog visible={this.state.metadataDialogVisible} title={metadataDialogTitle || 'Metadata'} footer={false} onClose={this.closeMetadataDialog} style={{ width: '80vw', height: '80vh', overflow: 'auto' }}>
          <pre>{JSON.stringify(this.state.selectedMetadata, null, 2)}</pre>
        </Dialog>
      </Page>
    );
  }
}

const mapStateToProps = (state: any) => ({
  locale: state.locale.locale,
});

export default connect(mapStateToProps)(withRouter(ConfigProvider.config(ClusterManager, {})));
