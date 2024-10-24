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
    configInfo: 'ConfigurationInfo',
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
    inputFilterPlaceholder: 'Please enter filter criteria',
    branchSessionSwitchLabel: 'Whether to include branch sessions',
    resetButtonLabel: 'Reset',
    searchButtonLabel: 'Search',
    operateTitle: 'operate',
    showBranchSessionTitle: 'View branch session',
    showGlobalLockTitle: 'View global lock',
    branchSessionDialogTitle: 'Branch session info',
  },
  GlobalLockInfo: {
    title: 'GlobalLockInfo',
    subTitle: 'list',
    createTimeLabel: 'CreateTime',
    inputFilterPlaceholder: 'Please enter filter criteria',
    resetButtonLabel: 'Reset',
    searchButtonLabel: 'Search',
  },
  ConfigInfo: {
    title: 'ConfigurationInfo',
    subTitle: 'list',
    resetButtonLabel: 'Reset',
    searchButtonLabel: 'Search',
    createButtonLabel: 'Create',
    editButtonLabel: 'Edit',
    deleteButtonLabel: 'Delete',
    clearButtonLabel: 'Clear',
    uploadButtonLabel: 'Upload',
    operateTitle: 'Actions',
    disableTitle: 'This page is only available if the Configuration Center type is raft mode',
    editTitle: 'Edit Config',
    deleteTitle: 'Delete Config',
    deleteConfirmLabel: 'Are you sure you want to delete the configuration item with key: ',
    deleteAllConfirmLabel: 'Are you sure you want to clear all configuration items in the following namespace and dataId: ',
    addTitle: 'Add Config',
    operationSuccess: 'Operation Success!',
    operationFailed: 'Operation Failed',
    inputFilterPlaceholder: 'Please select filter criteria',
    fieldFillingTips: 'Please fill in the required fields',
    uploadTitle: 'Upload Config',
    uploadFileButtonLabel: 'Upload File',
  },
};

export default enUs;
