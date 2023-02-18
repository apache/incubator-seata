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

const zhCn: ILocale = {
  MenuRouter: {
    overview: '概览',
    transactionInfo: '事务信息',
    globalLockInfo: '全局锁信息',
    sagaStatemachineDesigner: 'Saga状态机设计器',
  },
  Header: {
    home: '首页',
    cloud: 'Seata企业版',
    docs: '文档',
    blog: '博客',
    community: '社区',
    download: '下载',
    languageSwitchButton: 'En',
    logout: '登出',
  },
  Login: {
    login: '登录',
    submit: '提交',
    pleaseInputUsername: '请输入用户名',
    pleaseInputPassword: '请输入密码',
    invalidUsernameOrPassword: '用户名或密码错误',
    passwordRequired: '密码不能为空',
    usernameRequired: '用户名不能为空',
    desc: 'Seata 是一款开源的分布式事务解决方案，致力于在微服务架构下提供高性能和简单易用的分布式事务服务。',
  },
  Overview: {
    title: '概览',
    subTitle: '基础列表页',
    search: '搜索',
  },
  TransactionInfo: {
    title: '事务信息',
    subTitle: '基础列表页',
    createTimeLabel: '创建时间',
    selectFilerPlaceholder: '请选择筛选条件',
    inputFilterPlaceholder: '请输入筛选条件',
    branchSessionSwitchLabel: '是否包含分支事务',
    resetButtonLabel: '重置',
    searchButtonLabel: '搜索',
    operateTitle: '操作',
    showBranchSessionTitle: '查看分支信息',
    showGlobalLockTitle: '查看全局锁',
    branchSessionDialogTitle: '分支事务信息',
  },
  GlobalLockInfo: {
    title: '全局锁信息',
    subTitle: '基础列表页',
    createTimeLabel: '创建时间',
    inputFilterPlaceholder: '请输入筛选条件',
    resetButtonLabel: '重置',
    searchButtonLabel: '搜索',
  },
};

export default zhCn;
