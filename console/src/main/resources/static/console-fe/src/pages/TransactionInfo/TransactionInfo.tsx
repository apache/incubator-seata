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
import { ConfigProvider, Table, Button, DatePicker, Form, Icon, Switch, Pagination, Dialog, Input, Select, Message } from '@alicloud/console-components';
import Actions, { LinkButton } from '@alicloud/console-components-actions';
import { withRouter } from 'react-router-dom';
import { connect } from 'react-redux';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';
import getData, { changeGlobalData, deleteBranchData, deleteGlobalData, GlobalSessionParam, sendGlobalCommitOrRollback,
  startBranchData, startGlobalData, stopBranchData, stopGlobalData, forceDeleteGlobalData, forceDeleteBranchData, fetchNamespaceV2, addGroup, changeGroup } from '@/service/transactionInfo';
import moment from 'moment';

import './index.scss';
import { get as lodashGet} from "lodash";
import {enUsKey, getCurrentLanguage} from "@/reducers/locale";

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
  xid : string;
  currentBranchSession: Array<any>;
  globalSessionParam : GlobalSessionParam;
  namespaceOptions: Map<string, NamespaceData>;
  clusters: Array<string>;
  vgroups: Array<string>;
  createVGroupDialogVisible: boolean;
  vGroupName: string;
  changeVGroupDialogVisible: boolean;
  selectedVGroup: string;
  targetNamespace: string;
  targetClusters: Array<string>;
  targetCluster: string;
  targetUnits: Array<string>;
  targetUnit: string;
  originalNamespace: string;
  originalClusters: Array<string>;
  originalCluster: string;
  originalVGroups: Array<string>;
  createNamespace: string;
  createCluster: string;
  createUnits: Array<string>;
  createUnit: string;
}

type NamespaceData = { clusters: string[], clusterVgroups: {[key: string]: string[]}, clusterUnits: {[key: string]: string[]}, clusterTypes: {[key: string]: string} };

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
  {
    label: 'Deleting',
    value: 18,
    iconType: 'warning',
    iconColor: '#FFA003',
  },
  {
    label: 'StopCommitRetry',
    value: 19,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
  {
    label: 'StopRollbackRetry',
    value: 20,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
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
  {
    label: 'PhaseTwo_CommitFailed_XAER_NOTA_Retryable',
    value: 11,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'PhaseTwo_RollbackFailed_XAER_NOTA_Retryable',
    value: 12,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'PhaseOne_RDONLY',
    value: 13,
    iconType: 'error',
    iconColor: '#FF3333',
  },
  {
    label: 'Stop_Retry',
    value: 14,
    iconType: 'ellipsis',
    iconColor: 'rgb(3, 193, 253)',
  },
];

const commonWarnning = 'Global transaction commit or rollback inconsistency problem exists.'
const warnning = new Map([
  ['stopBranchSession', new Map([['AT', ''], ['XA', ''],
    ['TCC', 'Please check if this may affect the logic of other branches.'], ['SAGA', '']])],
  ['deleteBranchSession',
    new Map([['AT', 'The global lock and undo log will be deleted too, dirty write problem exists.'],
      ['XA', 'The xa branch will rollback'], ['TCC', ''], ['SAGA', '']])],
  ['deleteGlobalSession', new Map([['AT', ''], ['XA', ''], ['TCC', ''], ['SAGA', '']])],
  ['forceDeleteBranchSession',
    new Map([['AT', 'The force delete will only delete session in server.'],
      ['XA', 'The force delete will only delete session in server.'],
      ['TCC', 'The force delete will only delete session in server.'], ['SAGA', 'The force delete will only delete session in server.']])],
  ['forceDeleteGlobalSession', new Map([['AT', 'The force delete will only delete session in server.'],
    ['XA', 'The force delete will only delete session in server.'],
    ['TCC', 'The force delete will only delete session in server.'],
    ['SAGA', 'The force delete will only delete session in server.']])],
])

const VGROUP_REFRESH_DELAY_MS = 6000;

class TransactionInfo extends React.Component<GlobalProps, TransactionInfoState> {
  static displayName = 'TransactionInfo';

  state: TransactionInfoState = {
    list: [],
    total: 0,
    loading: false,
    branchSessionDialogVisible: false,
    xid: '',
    currentBranchSession: [],
    globalSessionParam: {
      withBranch: false,
      pageSize: 10,
      pageNum: 1,
    },
    namespaceOptions: new Map<string, NamespaceData>(),
    clusters: [],
    vgroups: [],
    createVGroupDialogVisible: false,
    vGroupName: '',
    changeVGroupDialogVisible: false,
    selectedVGroup: '',
    targetNamespace: '',
    targetClusters: [],
    targetCluster: '',
    targetUnits: [],
    targetUnit: '',
    originalNamespace: '',
    originalClusters: [],
    originalCluster: '',
    originalVGroups: [],
    createNamespace: '',
    createCluster: '',
    createUnits: [],
    createUnit: '',
  };
  componentDidMount = () => {
    // search once by default
    this.loadNamespaces();
  }
  loadNamespaces = async () => {
    try {
      const namespaces = await fetchNamespaceV2();
      const namespaceOptions = new Map<string, NamespaceData>();

      Object.keys(namespaces).forEach(namespaceKey => {
        const namespaceData = namespaces[namespaceKey];
        const clustersData = namespaceData.clusters || {};
        const clusterVgroups: {[key: string]: string[]} = {};
        const clusterUnits: {[key: string]: string[]} = {};
        const clusterTypes: {[key: string]: string} = {};
        Object.keys(clustersData).forEach(clusterName => {
          const cluster = clustersData[clusterName];
          clusterVgroups[clusterName] = cluster.vgroups || [];
          clusterUnits[clusterName] = cluster.units || [];
          clusterTypes[clusterName] = cluster.type || 'default';
        });
        const clusters = Object.keys(clustersData);
        namespaceOptions.set(namespaceKey, {
          clusters,
          clusterVgroups,
          clusterUnits,
          clusterTypes,
        });
      });
        if (namespaceOptions.size > 0) {
            // Set default namespace to the first option
            const firstNamespace = Array.from(namespaceOptions.keys())[0];
            const selectedNamespace = namespaceOptions.get(firstNamespace);
            const firstCluster = selectedNamespace ? selectedNamespace.clusters[0] : undefined;
            const clusterVgroups = selectedNamespace ? selectedNamespace.clusterVgroups : {};
            const selectedVgroups = firstCluster ? clusterVgroups[firstCluster] || [] : [];
            this.setState(prevState => ({
                namespaceOptions,
                globalSessionParam: {
                    ...prevState.globalSessionParam,
                    namespace: firstNamespace,
                    cluster: firstCluster,
                },
                clusters: selectedNamespace ? selectedNamespace.clusters : [],
                vgroups: selectedVgroups,
            }));
             this.search();
         } else {
             this.setState({
                 namespaceOptions,
             });
         }
    } catch (error) {
      console.error('Failed to fetch namespaces:', error);
    }
  }
  resetSearchFilter = () => {
    this.setState(prevState => ({
      globalSessionParam: {
        withBranch: false,
        // pagination info don`t reset
        pageSize: prevState.globalSessionParam.pageSize,
        pageNum: prevState.globalSessionParam.pageNum,
      },
      clusters: [],
      vgroups: [],
    }));
  }

  search = () => {
    this.setState({ loading: true });
    const currentBranchSessionDialogVisible = this.state.branchSessionDialogVisible;
    const currentXid = this.state.xid;
    getData(this.state.globalSessionParam).then(data => {
      // if the result set is empty, set the page number to go back to the first page
      if (data.total === 0) {
        this.setState(prevState => ({
          list: [],
          total: 0,
          loading: false,
          globalSessionParam: { ...prevState.globalSessionParam, pageNum: 1 },
        }));
        return;
      }
      // format time
      data.data.forEach((element: any) => {
        element.beginTime = (element.beginTime == null || element.beginTime === '') ? null : moment(Number(element.beginTime)).format('YYYY-MM-DD HH:mm:ss');
        element.cluster = this.state.globalSessionParam.cluster;
        element.namespace = this.state.globalSessionParam.namespace;
        element.vgroup = this.state.globalSessionParam.vgroup;
        if (element.branchSessionVOs != null) {
          element.branchSessionVOs.forEach((element: any) => {
            element.cluster = this.state.globalSessionParam.cluster;
            element.namespace = this.state.globalSessionParam.namespace;
            element.vgroup = this.state.globalSessionParam.vgroup;
          });
        }
      });

      if (currentBranchSessionDialogVisible) {
        const currentBranchSession = data.data.find((item: any) => item.xid == currentXid)?.branchSessionVOs || [];
        this.setState({
          list: data.data,
          total: data.total,
          loading: false,
          currentBranchSession,
        });
      } else {
        this.setState({
          list: data.data,
          total: data.total,
          loading: false,
        });
      }
    }).catch(() => {
      this.setState({ loading: false });
    });
  }

  searchFilterOnChange = (key: string, val: string) => {
    if (key === 'namespace') {
      const selectedNamespace = this.state.namespaceOptions.get(val);
      const clusters = selectedNamespace ? selectedNamespace.clusters : [];
      const firstCluster = clusters.length > 0 ? clusters[0] : undefined;
      const clusterVgroups = selectedNamespace ? selectedNamespace.clusterVgroups : {};
      const vgroups = firstCluster ? clusterVgroups[firstCluster] || [] : [];
      this.setState(prevState => ({
        clusters,
        vgroups,
        globalSessionParam: { ...prevState.globalSessionParam, [key]: val, cluster: firstCluster, vgroup: '' },
      }));
    } else if (key === 'cluster') {
      const currentNamespace = this.state.globalSessionParam.namespace;
      if (currentNamespace) {
        const namespaceData = this.state.namespaceOptions.get(currentNamespace);
        const clusterVgroups = namespaceData ? namespaceData.clusterVgroups : {};
        const selectedVgroups = clusterVgroups[val] || [];
        this.setState(prevState => ({
          vgroups: selectedVgroups,
          globalSessionParam: { ...prevState.globalSessionParam, [key]: val, vgroup: '' },
        }));
      } else {
        this.setState(prevState => ({
          globalSessionParam: { ...prevState.globalSessionParam, [key]: val, vgroup: '' },
        }));
      }
    } else {
      this.setState(prevState => ({
        globalSessionParam: { ...prevState.globalSessionParam, [key]: val },
      }));
    }
  };

  branchSessionSwitchOnChange = (checked: boolean, _e?: any) => {
    this.setState(prevState => ({
      globalSessionParam: { ...prevState.globalSessionParam, withBranch: checked },
    }));
    if (checked) {
      // if checked, do search for load branch sessions
      this.search();
    }
  }

  createTimeOnChange = (value: Array<any>) => {
    // timestamp(milliseconds)
    const timeStart = value[0] == null ? undefined : moment(value[0]).unix() * 1000;
    const timeEnd = value[1] == null ? undefined : moment(value[1]).unix() * 1000;
    this.setState(prevState => ({
      globalSessionParam: { ...prevState.globalSessionParam, timeStart, timeEnd },
    }));
  }

  statusCell = (val: number, _index?: number, _record?: any) => {
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

  branchSessionStatusCell = (val: number, _index?: number, _record?: any) => {
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
    const { locale, history } = this.props;
    const {
      showBranchSessionTitle,
      showGlobalLockTitle,
      deleteGlobalSessionTitle,
      forceDeleteGlobalSessionTitle,
      stopGlobalSessionTitle,
      startGlobalSessionTitle,
      sendGlobalSessionTitle,
      changeGlobalSessionTitle,
    } = locale?.TransactionInfo || {};
    let width = getCurrentLanguage() === enUsKey ? '450px' : '420px'
    let height = '120px';
    return (
      <Actions style={{ width: width,
        height: height,
      }} threshold = {8} wrap = {true} >
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
              query: {
                xid: record.xid,
                vgroup: record.vgroup,
                namespace: this.state.globalSessionParam.namespace,
                cluster: this.state.globalSessionParam.cluster
              },
            });
          }}
        >
          {showGlobalLockTitle}
        </LinkButton>


        <Button
          onClick={() => {
            Dialog.confirm({
              title: 'Confirm',
              content: 'Are you sure you want to delete global transactions',
              onOk: () => {
                const warnMessageMap = warnning.get('deleteGlobalSession')
                let warnMessage = ''
                if (warnMessageMap != null) {
                  for (const [key, value] of warnMessageMap) {
                    if (value == '') {
                      continue
                    }
                    warnMessage += key + ':' + '<br>' + value + '<br>'
                  }
                }
                Dialog.confirm({
                  title: 'Warnning',
                  content: <div dangerouslySetInnerHTML={{__html: commonWarnning + '<br>' + warnMessage}}/>,
                  onOk: () => {
                    deleteGlobalData(record).then(() => {
                      Message.success("Delete successfully")
                      this.search()
                    }).catch((rsp) => {
                      Message.error(lodashGet(rsp, 'data.message'))
                    })
                  }
                });
              }
            });
          }}
        >
          {deleteGlobalSessionTitle}
        </Button>

        <Button
          onClick={() => {
            Dialog.confirm({
              title: 'Confirm',
              content: 'Are you sure you want to force delete global transactions',
              onOk: () => {
                const warnMessageMap = warnning.get('forceDeleteGlobalSession')
                let warnMessage = ''
                if (warnMessageMap != null) {
                  for (const [key, value] of warnMessageMap) {
                    if (value == '') {
                      continue
                    }
                    warnMessage += key + ':' + '<br>' + value + '<br>'
                  }
                }
                Dialog.confirm({
                  title: 'Warnning',
                  content: <div dangerouslySetInnerHTML={{__html: commonWarnning + '<br>' + warnMessage}}/>,
                  onOk: () => {
                    forceDeleteGlobalData(record).then(() => {
                      Message.success("Delete successfully")
                      this.search()
                    }).catch((rsp) => {
                      Message.error(lodashGet(rsp, 'data.message'))
                    })
                  }
                });
              }
            });
          }}
        >
          {forceDeleteGlobalSessionTitle}
        </Button>

        {record.status == 19 || record.status == 20 ? (
          <Button
            onClick={() => {
              Dialog.confirm({
                title: 'Confirm',
                content: 'Are you sure you want to start restart global transactions',
                onOk: () => {
                  startGlobalData(record).then(() => {
                    Message.success("Start successfully")
                    this.search()
                  }).catch((rsp) => {
                    Message.error(lodashGet(rsp, 'data.message'))
                  })
                }
              });
            }}
          >
            {startGlobalSessionTitle}
          </Button>
        ) : (<Button onClick={() => {
          Dialog.confirm({
            title: 'Confirm',
            content: 'Are you sure you want to stop stop global transactions',
            onOk: () => {
              stopGlobalData(record).then(() => {
                Message.success("Stop successfully")
                this.search()
              }).catch((rsp) => {
                Message.error(lodashGet(rsp, 'data.message'))
              })
            }
          });
        }}
        >
          {stopGlobalSessionTitle}
        </Button>)
        }

        <Button
          onClick={() => {
            Dialog.confirm({
              title: 'Confirm',
              content: 'Are you sure you want to send commit or rollback to global transactions',
              onOk: () => {
                sendGlobalCommitOrRollback(record).then(() => {
                  Message.success("Send successfully")
                  this.search()
                }).catch((rsp) => {
                  Message.error(lodashGet(rsp, 'data.message'))
                })
              }
            });
          }}
        >
          {sendGlobalSessionTitle}
        </Button>

        <Button
          onClick={() => {
            Dialog.confirm({
              title: 'Confirm',
              content: 'Are you sure you want to change the global transactions status',
              onOk: () => {
                changeGlobalData(record).then(() => {
                  Message.success("Change successfully")
                  this.search()
                }).catch((rsp) => {
                  Message.error(lodashGet(rsp, 'data.message'))
                })
              }
            });

          }}
        >
          {changeGlobalSessionTitle}
        </Button>
      </Actions>);
  }

  branchSessionDialogOperateCell = (val: string, index: number, record: any) => {
    const { locale, history } = this.props;
    const {
      showGlobalLockTitle,
      deleteBranchSessionTitle,
      stopBranchSessionTitle,
      startBranchSessionTitle,
      forceDeleteBranchSessionTitle,
    } = locale.TransactionInfo || {};
    let width = getCurrentLanguage() === enUsKey ? '500px' : '450px'
    let height = '120px';
    return (
      <Actions style={{ width: width,
        height: height,
      }} threshold = {8} wrap = {true} >
        <LinkButton
          onClick={() => {
            history.push({
              pathname: '/globallock/list',
              // @ts-ignore
              query: {
                xid: record.xid,
                vgroup: record.vgroup,
                namespace: this.state.globalSessionParam.namespace,
                cluster: this.state.globalSessionParam.cluster
              },
            });
          }}
        >
          {showGlobalLockTitle}
        </LinkButton>


        <Button
          onClick={() => {
            Dialog.confirm({
              title: 'Confirm',
              content: 'Are you sure you want to delete branch transactions',
              onOk: () => {
                let warnMessage = warnning.get('deleteBranchSession')!.get(record.branchType);
                Dialog.confirm({
                  title: 'Warnning',
                  content: <div dangerouslySetInnerHTML={{__html: commonWarnning + '<br>' + warnMessage}}/>,
                  onOk: () => {
                    deleteBranchData(record).then(() => {
                      Message.success("Delete successfully")
                      this.search()
                    }).catch((rsp) => {
                      Message.error(lodashGet(rsp, 'data.message'))
                    })
                  }
                });
              }
            });
          }}
        >
          {deleteBranchSessionTitle}
        </Button>

        <Button
          onClick={() => {
            Dialog.confirm({
              title: 'Confirm',
              content: 'Are you sure you want to force delete branch transactions',
              onOk: () => {
                let warnMessage = warnning.get('forceDeleteBranchSession')!.get(record.branchType);
                Dialog.confirm({
                  title: 'Warnning',
                  content: <div dangerouslySetInnerHTML={{__html: commonWarnning + '<br>' + warnMessage}}/>,
                  onOk: () => {
                    forceDeleteBranchData(record).then(() => {
                      Message.success("Delete successfully")
                      this.search()
                    }).catch((rsp) => {
                      Message.error(lodashGet(rsp, 'data.message'))
                    })
                  }
                });
              }
            });
          }}
        >
          {forceDeleteBranchSessionTitle}
        </Button>

        {record.status == 14 ? (
          <Button
            onClick={() => {
              Dialog.confirm({
                title: 'Confirm',
                content: 'Are you sure you want to start branch transactions retry',
                onOk: () => {
                  startBranchData(record).then(() => {
                    Message.success("Start successfully")
                    this.search()
                  }).catch((rsp) => {
                    Message.error(lodashGet(rsp, 'data.message'))
                  })
                }
              });
            }}
          >
            {startBranchSessionTitle}
          </Button>
        ) : <Button
          onClick={() => {
            Dialog.confirm({
              title: 'Confirm',
              content: 'Are you sure you want to stop branch transactions retry',
              onOk: () => {
                let warnMessage = warnning.get('stopBranchSession')!.get(record.branchType);
                Dialog.confirm({
                  title: 'Warnning',
                  content: <div dangerouslySetInnerHTML={{__html: commonWarnning + '<br>' + warnMessage}}/>,
                  onOk: () => {
                    stopBranchData(record).then(() => {
                      Message.success("Stop successfully")
                      this.search()
                    }).catch((rsp) => {
                      Message.error(lodashGet(rsp, 'data.message'))
                    })
                  }
                });
              }
            });
          }}
        >
          {stopBranchSessionTitle}
        </Button>}

      </Actions>);
  }

  paginationOnChange = (current: number, _e?: any) => {
    this.setState(prevState => ({
      globalSessionParam: { ...prevState.globalSessionParam, pageNum: current },
    }));
    this.search();
  }

  paginationOnPageSizeChange = (pageSize: number) => {
    this.setState(prevState => ({
      globalSessionParam: { ...prevState.globalSessionParam, pageSize },
    }));
    this.search();
  }

  showBranchSessionDialog = (val: string, index: number, record: any) => () => {
    this.setState({
      branchSessionDialogVisible: true,
      currentBranchSession: record.branchSessionVOs,
      xid: record.xid,
    });
  }

  closeBranchSessionDialog = () => {
    this.setState({
      branchSessionDialogVisible: false,
      currentBranchSession: [],
      xid: '',
    });
  }

  showCreateVGroupDialog = () => {
    this.setState(prevState => {
      const clusterUnits = prevState.globalSessionParam.namespace ? prevState.namespaceOptions.get(prevState.globalSessionParam.namespace)?.clusterUnits || {} : {};
      const units = prevState.globalSessionParam.cluster ? clusterUnits[prevState.globalSessionParam.cluster] || [] : [];
      return {
        createVGroupDialogVisible: true,
        vGroupName: '',
        createNamespace: prevState.globalSessionParam.namespace || '',
        createCluster: prevState.globalSessionParam.cluster || '',
        createUnits: units,
        createUnit: units.length > 0 ? units[0] : '',
      };
    });
  }

  closeCreateVGroupDialog = () => {
    this.setState({
      createVGroupDialogVisible: false,
      vGroupName: '',
      createNamespace: '',
      createCluster: '',
      createUnits: [],
      createUnit: '',
    });
  }

  showChangeVGroupDialog = () => {
    this.setState({
      changeVGroupDialogVisible: true,
      selectedVGroup: '',
      targetNamespace: '',
      targetClusters: [],
      targetCluster: '',
      targetUnits: [],
      targetUnit: '',
      originalNamespace: '',
      originalClusters: [],
      originalCluster: '',
      originalVGroups: [],
    });
  }

  closeChangeVGroupDialog = () => {
    this.setState({
      changeVGroupDialogVisible: false,
      selectedVGroup: '',
      targetNamespace: '',
      targetClusters: [],
      targetCluster: '',
      targetUnits: [],
      targetUnit: '',
      originalNamespace: '',
      originalClusters: [],
      originalCluster: '',
      originalVGroups: [],
    });
  }

  handleCreateVGroup = () => {
    const { locale } = this.props;
    const { createVGroupErrorMessage, createVGroupSuccessMessage, createVGroupFailMessage } = locale.TransactionInfo || {};
    const { createNamespace, createCluster, vGroupName, createUnit } = this.state;
    if (!createNamespace || !createCluster || !vGroupName.trim()) {
      Message.error(createVGroupErrorMessage);
      return;
    }

    const clusterType = this.state.namespaceOptions.get(createNamespace)?.clusterTypes[createCluster];
    const unitName = clusterType !== 'default' ? createUnit : '';

    addGroup(createNamespace, createCluster, vGroupName.trim(), unitName).then(() => {
      Message.success(createVGroupSuccessMessage);
      this.closeCreateVGroupDialog();
      // Delay 5 seconds before reloading namespaces to get the latest vgroup list
      setTimeout(() => {
        this.loadNamespaces();
      }, VGROUP_REFRESH_DELAY_MS);
    }).catch((error) => {
      const backendMessage = lodashGet(error, 'data.message');
      const displayMessage = backendMessage ? `${createVGroupFailMessage}: ${backendMessage}` : createVGroupFailMessage;
      Message.error(displayMessage);
    });
  }

    handleChangeVGroup = () => {
    const { locale } = this.props;
    const { changeVGroupSuccessMessage, changeVGroupFailMessage } = locale.TransactionInfo || {};
    const { selectedVGroup, targetNamespace, targetCluster, targetUnit } = this.state;
    if (!selectedVGroup || !targetNamespace || !targetCluster) {
      return;
    }

    const targetClusterType = this.state.namespaceOptions.get(targetNamespace)?.clusterTypes[targetCluster];
    const unitName = targetClusterType !== 'default' ? targetUnit : '';

    changeGroup(targetNamespace, targetCluster, selectedVGroup, unitName).then(() => {
      Message.success(changeVGroupSuccessMessage);
      this.closeChangeVGroupDialog();
      // Delay 5 seconds before reloading namespaces to get the latest vgroup list
      setTimeout(() => {
        this.loadNamespaces();
      }, VGROUP_REFRESH_DELAY_MS);
    }).catch((error) => {
      const backendMessage = lodashGet(error, 'data.message');
      const displayMessage = backendMessage ? `${changeVGroupFailMessage}: ${backendMessage}` : changeVGroupFailMessage;
      Message.error(displayMessage);
    });
    }

  isChangeVGroupDisabled = (): boolean => {
    const { selectedVGroup, targetNamespace, targetCluster, targetUnit, namespaceOptions } = this.state;
    if (!selectedVGroup || !targetNamespace || !targetCluster) {
      return true;
    }
    const clusterType = namespaceOptions.get(targetNamespace)?.clusterTypes[targetCluster];
    return clusterType !== 'default' && !targetUnit;
  }

  render() {
    const { locale } = this.props;
    const transactionInfo = locale.TransactionInfo || {};
    const { title, subTitle, createTimeLabel,
      selectFilerPlaceholder,
      selectNamespaceFilerPlaceholder,
      selectClusterFilerPlaceholder,
      selectVGroupFilerPlaceholder,
      inputFilterPlaceholder,
      branchSessionSwitchLabel,
      resetButtonLabel,
      searchButtonLabel,
      operateTitle,
      branchSessionDialogTitle,
      createVGroupButtonLabel,
      createVGroupDialogTitle,
      createVGroupInputPlaceholder,
      createVGroupConfirmButton,
      changeVGroupButtonLabel,
      changeVGroupDialogTitle,
      namespaceLabel,
      clusterLabel,
      originalNamespaceLabel,
      originalClusterLabel,
      selectVGroupLabel,
      targetNamespaceLabel,
      targetClusterLabel,
      vGroupNameLabel,
      confirmButtonLabel,
      selectOriginalNamespacePlaceholder,
      selectOriginalClusterPlaceholder,
      selectTargetNamespacePlaceholder,
      selectTargetClusterPlaceholder,
      selectVGroupPlaceholder,
    } = transactionInfo;
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
          <FormItem name="namespace" label="namespace">
            <Select
                hasClear
                placeholder={selectNamespaceFilerPlaceholder}
                onChange={(value: string) => {
                  this.searchFilterOnChange('namespace', value);
                }}
                dataSource={Array.from(this.state.namespaceOptions.keys()).map(key => ({ label: key, value: key }))}
                value={this.state.globalSessionParam.namespace}
            />
          </FormItem>
          <FormItem name="cluster" label="cluster">
            <Select
                hasClear
                placeholder={selectClusterFilerPlaceholder}
                onChange={(value: string) => {
                  this.searchFilterOnChange('cluster', value);
                }}
                dataSource={this.state.clusters.map(value => ({ label: value, value }))}
                value={this.state.globalSessionParam.cluster}
            />
          </FormItem>
          <FormItem name="vgroup" label="vgroup">
            <Select
                hasClear
                placeholder={selectVGroupFilerPlaceholder}
                onChange={(value: string) => {
                  this.searchFilterOnChange('vgroup', value);
                }}
                dataSource={this.state.vgroups.map(value => ({ label: value, value }))}
                value={this.state.globalSessionParam.vgroup}
                key={this.state.globalSessionParam.cluster}
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
          {/* {create vgroup button} */}
          <FormItem>
            <Button onClick={this.showCreateVGroupDialog}>
              {createVGroupButtonLabel}
            </Button>
          </FormItem>
          {/* {change vgroup button} */}
          <FormItem>
            <Button onClick={this.showChangeVGroupDialog}>
              {changeVGroupButtonLabel}
            </Button>
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
          <Table dataSource={this.state.currentBranchSession} loading={this.state.loading} style={{ overflowX: 'auto' }} >
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

        {/* create vgroup dialog */}
        <Dialog visible={this.state.createVGroupDialogVisible} title={createVGroupDialogTitle} footer={false} onClose={this.closeCreateVGroupDialog}>
          <Form inline labelAlign="left">
            <FormItem name="createNamespace" label={namespaceLabel}>
              <Select
                placeholder={selectNamespaceFilerPlaceholder}
                onChange={(value: string) => {
                  this.setState(prevState => {
                    const clusters = value ? prevState.namespaceOptions.get(value)?.clusters || [] : [];
                    const clusterUnits = prevState.namespaceOptions.get(value)?.clusterUnits || {};
                    const units = clusters.length > 0 ? clusterUnits[clusters[0]] || [] : [];
                    return {
                      createNamespace: value,
                      createCluster: clusters.length > 0 ? clusters[0] : '',
                      createUnits: units,
                      createUnit: units.length > 0 ? units[0] : '',
                    };
                  });
                }}
                dataSource={Array.from(this.state.namespaceOptions.keys()).map(key => ({ label: key, value: key }))}
                value={this.state.createNamespace}
              />
            </FormItem>
            <FormItem name="createCluster" label={clusterLabel}>
              <Select
                placeholder={selectClusterFilerPlaceholder}
                onChange={(value: string) => {
                  this.setState(prevState => {
                    const namespaceData = prevState.namespaceOptions.get(prevState.createNamespace);
                    const clusterUnits = namespaceData ? namespaceData.clusterUnits : {};
                    const units = clusterUnits[value] || [];
                    return {
                      createCluster: value,
                      createUnits: units,
                      createUnit: units.length > 0 ? units[0] : '',
                    };
                  });
                }}
                dataSource={this.state.namespaceOptions.get(this.state.createNamespace)?.clusters.map(value => ({ label: value, value })) || []}
                value={this.state.createCluster}
              />
            </FormItem>
            {this.state.namespaceOptions.get(this.state.createNamespace)?.clusterTypes[this.state.createCluster] !== 'default' && (
              <FormItem name="createUnit" label="Unit">
                <Select
                  placeholder="Select unit"
                  onChange={(value: string) => {
                    this.setState({ createUnit: value });
                  }}
                  dataSource={this.state.createUnits.map(value => ({ label: value, value }))}
                  value={this.state.createUnit}
                />
              </FormItem>
            )}
            <FormItem name="vGroupName" label={vGroupNameLabel}>
              <Input
                placeholder={createVGroupInputPlaceholder}
                value={this.state.vGroupName}
                onChange={(value: string) => { this.setState({ vGroupName: value }); }}
              />
            </FormItem>
            <FormItem>
              <Button type="primary" onClick={this.handleCreateVGroup}>
                {createVGroupConfirmButton}
              </Button>
            </FormItem>
          </Form>
        </Dialog>

        {/* change vgroup dialog */}
        <Dialog visible={this.state.changeVGroupDialogVisible} title={changeVGroupDialogTitle} footer={false} onClose={this.closeChangeVGroupDialog}>
          <Form inline labelAlign="left">
            <FormItem name="originalNamespace" label={originalNamespaceLabel}>
              <Select
                hasClear
                placeholder={selectOriginalNamespacePlaceholder}
                onChange={(value: string) => {
                  this.setState(prevState => {
                    const clusters = value ? prevState.namespaceOptions.get(value)?.clusters || [] : [];
                    const firstCluster = clusters.length > 0 ? clusters[0] : '';
                    const namespaceData = prevState.namespaceOptions.get(value);
                    const clusterVgroups = namespaceData ? namespaceData.clusterVgroups : {};
                    const vgroups = firstCluster ? clusterVgroups[firstCluster] || [] : [];
                    return {
                      originalNamespace: value,
                      originalClusters: clusters,
                      originalCluster: firstCluster,
                      originalVGroups: vgroups,
                    };
                  });
                }}
                dataSource={Array.from(this.state.namespaceOptions.keys()).map(key => ({ label: key, value: key }))}
                value={this.state.originalNamespace}
              />
            </FormItem>
            <FormItem name="originalCluster" label={originalClusterLabel}>
              <Select
                hasClear
                placeholder={selectOriginalClusterPlaceholder}
                onChange={(value: string) => {
                  this.setState(prevState => {
                    const namespaceData = prevState.namespaceOptions.get(prevState.originalNamespace);
                    const clusterVgroups = namespaceData ? namespaceData.clusterVgroups : {};
                    const vgroups = clusterVgroups[value] || [];
                    return {
                      originalCluster: value,
                      originalVGroups: vgroups,
                    };
                  });
                }}
                dataSource={this.state.originalClusters.map(value => ({ label: value, value }))}
                value={this.state.originalCluster}
              />
            </FormItem>
            <FormItem name="selectedVGroup" label={selectVGroupLabel}>
              <Select
                hasClear
                placeholder={selectVGroupPlaceholder}
                onChange={(value: string) => {
                  this.setState({ selectedVGroup: value });
                }}
                dataSource={this.state.originalVGroups.map(value => ({ label: value, value }))}
                value={this.state.selectedVGroup}
              />
            </FormItem>
            <FormItem name="targetNamespace" label={targetNamespaceLabel}>
              <Select
                hasClear
                placeholder={selectTargetNamespacePlaceholder}
                onChange={(value: string) => {
                  this.setState(prevState => {
                    const clusters = value ? prevState.namespaceOptions.get(value)?.clusters || [] : [];
                    return {
                      targetNamespace: value,
                      targetClusters: clusters,
                      targetCluster: '',
                      targetUnits: [],
                      targetUnit: '',
                    };
                  });
                }}
                dataSource={Array.from(this.state.namespaceOptions.keys()).map(key => ({ label: key, value: key }))}
                value={this.state.targetNamespace}
              />
            </FormItem>
            <FormItem name="targetCluster" label={targetClusterLabel}>
              <Select
                hasClear
                placeholder={selectTargetClusterPlaceholder}
                onChange={(value: string) => {
                  this.setState(prevState => {
                    const namespaceData = prevState.namespaceOptions.get(prevState.targetNamespace);
                    const clusterUnits = namespaceData ? namespaceData.clusterUnits : {};
                    const units = clusterUnits[value] || [];
                    return {
                      targetCluster: value,
                      targetUnits: units,
                      targetUnit: units.length > 0 ? units[0] : '',
                    };
                  });
                }}
                dataSource={this.state.targetClusters.map(value => ({ label: value, value }))}
                value={this.state.targetCluster}
              />
            </FormItem>
            {this.state.targetCluster && this.state.namespaceOptions.get(this.state.targetNamespace)?.clusterTypes[this.state.targetCluster] !== 'default' && (
              <FormItem name="targetUnit" label="Target Unit">
                <Select
                  hasClear
                  placeholder="Select Target Unit"
                  onChange={(value: string) => {
                    this.setState({ targetUnit: value });
                  }}
                  dataSource={this.state.targetUnits.map(value => ({ label: value, value }))}
                  value={this.state.targetUnit}
                />
              </FormItem>
            )}
            <FormItem>
              <Button type="primary" onClick={this.handleChangeVGroup} disabled={this.isChangeVGroupDisabled()}>
                {confirmButtonLabel}
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

export default ConfigProvider.config(withRouter(connect(mapStateToProps)(TransactionInfo)), {});
