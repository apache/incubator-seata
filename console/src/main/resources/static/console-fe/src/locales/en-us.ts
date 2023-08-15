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
    cloud: 'CLOUD',
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
};

export default enUs;
