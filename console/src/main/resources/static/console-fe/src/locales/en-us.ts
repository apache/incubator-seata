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

import { ILocale } from './index.d';

const enUs: ILocale = {
  MenuRouter: {
    overview: 'Overview',
    transactionInfo: 'TransactionInfo',
    globalLockInfo: 'GlobalLockInfo',
    sagaStatemachineDesigner: 'SagaStatemachineDesigner',
  },
  Header: {
    home: 'HOME',
    docs: 'DOCS',
    blog: 'BLOG',
    community: 'COMMUNITY',
    download: 'DOWNLOAD',
    languageSwitchButton: '中',
    logout: 'logout',
    passwordRequired: 'password should not be empty',
    usernameRequired: 'username should not be empty',
  },
  Login: {
    login: 'Login',
    submit: 'Submit',
    pleaseInputUsername: 'Please input username',
    pleaseInputPassword: 'Please input password',
    invalidUsernameOrPassword: 'invalid username or password',
    desc: 'Seata is an open source distributed transaction solution that delivers high performance and easy to use distributed transaction services under a microservices architecture.',
  },
  Overview: {
    title: 'Overview',
    subTitle: 'list',
    search: 'search',
  },
  TransactionInfo: {
    title: 'TransactionInfo',
    subTitle: 'list',
    createTimeLabel: 'CreateTime',
    selectFilerPlaceholder: 'Please select filter criteria',
    selectNamespaceFilerPlaceholder: 'Please select namespace',
    selectClusterFilerPlaceholder: 'Please select cluster',
    selectVGroupFilerPlaceholder: 'Please select vgroup',
    inputFilterPlaceholder: 'Please enter filter criteria',
    branchSessionSwitchLabel: 'Whether to include branch sessions',
    resetButtonLabel: 'Reset',
    searchButtonLabel: 'Search',
    operateTitle: 'operate',
    showBranchSessionTitle: 'View branch session',
    showGlobalLockTitle: 'View global lock',
    branchSessionDialogTitle: 'Branch session info',
    deleteGlobalSessionTitle: 'Delete global session',
    forceDeleteGlobalSessionTitle: 'Force delete global session',
    stopGlobalSessionTitle: 'Stop global session retry',
    startGlobalSessionTitle: 'Start global session retry',
    sendGlobalSessionTitle: 'Commit or rollback global session',
    changeGlobalSessionTitle: 'Change global session status',
    deleteBranchSessionTitle: 'Delete branch session',
    forceDeleteBranchSessionTitle: 'force delete branch session',
    stopBranchSessionTitle: 'Stop branch session retry',
    startBranchSessionTitle: 'Start branch session retry',
    createVGroupButtonLabel: 'Create VGroup',
    createVGroupDialogTitle: 'Create VGroup',
    createVGroupInputPlaceholder: 'Enter VGroup name',
    createVGroupConfirmButton: 'Create',
    createVGroupErrorMessage: 'Please select namespace, cluster and enter vgroup name',
    createVGroupSuccessMessage: 'VGroup created successfully',
    createVGroupFailMessage: 'Failed to create vgroup',
    changeVGroupButtonLabel: 'Change VGroup',
    changeVGroupDialogTitle: 'Change VGroup',
    changeVGroupSuccessMessage: 'VGroup changed successfully',
    changeVGroupFailMessage: 'Failed to change vgroup',
    namespaceLabel: 'Namespace',
    clusterLabel: 'Cluster',
    originalNamespaceLabel: 'Original Namespace',
    originalClusterLabel: 'Original Cluster',
    selectVGroupLabel: 'Select VGroup',
    targetNamespaceLabel: 'Target Namespace',
    targetClusterLabel: 'Target Cluster',
    vGroupNameLabel: 'VGroup Name',
    confirmButtonLabel: 'Confirm',
    selectOriginalNamespacePlaceholder: 'Select original namespace',
    selectOriginalClusterPlaceholder: 'Select original cluster',
    selectTargetNamespacePlaceholder: 'Select target namespace',
    selectTargetClusterPlaceholder: 'Select target cluster',
    selectVGroupPlaceholder: 'Select vgroup',
  },
  GlobalLockInfo: {
    title: 'GlobalLockInfo',
    subTitle: 'list',
    createTimeLabel: 'CreateTime',
    inputFilterPlaceholder: 'Please enter filter criteria',
    selectNamespaceFilerPlaceholder: 'Please select namespace',
    selectClusterFilerPlaceholder: 'Please select cluster',
    selectVGroupFilerPlaceholder: 'Please select vgroup',
    resetButtonLabel: 'Reset',
    searchButtonLabel: 'Search',
    operateTitle: 'operate',
    deleteGlobalLockTitle: 'Delete global lock',
  },
  codeMessage: {
    200: 'The server successfully returned the requested data.',
    201: 'New or modified data successful.',
    202: 'A request has entered the background queue (asynchronous task).',
    204: 'Data deleted successfully.',
    400: 'The request was made with an error, and the server did not create or modify data.',
    401: 'The user does not have permission (token, username, password error).',
    403: 'The user is authorized, but access is forbidden.',
    404: 'The request is for a record that does not exist, and the server did not operate.',
    406: 'The requested format is not available.',
    410: 'The requested resource is permanently deleted and will not be obtained again.',
    422: 'A validation error occurred when creating an object.',
    500: 'An error occurred on the server, please check the server.',
    502: 'Gateway error.',
    503: 'Service unavailable, server temporarily overloaded or under maintenance.',
    504: 'Gateway timeout.',
    '-1000': 'Project name already exists, please use another name',
  },
};

export default enUs;
