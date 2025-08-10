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

const zhCn: ILocale = {
  MenuRouter: {
    overview: '概览',
    transactionInfo: '事务信息',
    globalLockInfo: '全局锁信息',
    configInfo:'配置信息',
    sagaStatemachineDesigner: 'Saga状态机设计器',
  },
  Header: {
    home: '首页',
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
    selectNamespaceFilerPlaceholder: '请选择命名空间',
    selectClusterFilerPlaceholder: '请选择集群',
    selectVGroupFilerPlaceholder: '请选择事务分组',
    inputFilterPlaceholder: '请输入筛选条件',
    branchSessionSwitchLabel: '是否包含分支事务',
    resetButtonLabel: '重置',
    searchButtonLabel: '搜索',
    operateTitle: '操作',
    showBranchSessionTitle: '查看分支信息',
    showGlobalLockTitle: '查看全局锁',
    branchSessionDialogTitle: '分支事务信息',
    deleteGlobalSessionTitle: '删除全局事务',
    forceDeleteGlobalSessionTitle: '强制删除全局事务',
    stopGlobalSessionTitle: '停止全局事务重试',
    startGlobalSessionTitle: '开启全局事务重试',
    sendGlobalSessionTitle: '提交或回滚全局事务',
    changeGlobalSessionTitle: '更新全局事务状态',
    deleteBranchSessionTitle: '删除分支事务',
    forceDeleteBranchSessionTitle: '强制删除分支事务',
    stopBranchSessionTitle: '停止分支事务重启',
    startBranchSessionTitle: '开启分支事务重试',
  },
  GlobalLockInfo: {
    title: '全局锁信息',
    subTitle: '基础列表页',
    createTimeLabel: '创建时间',
    inputFilterPlaceholder: '请输入筛选条件',
    selectNamespaceFilerPlaceholder: '请选择命名空间',
    selectClusterFilerPlaceholder: '请选择集群',
    selectVGroupFilerPlaceholder: '请选择事务分组',
    resetButtonLabel: '重置',
    searchButtonLabel: '搜索',
    operateTitle: '操作',
    deleteGlobalLockTitle: '删除全局锁',
  },
  ConfigInfo: {
    title: '配置信息',
    subTitle: '配置列表页',
    resetButtonLabel: '重置',
    searchButtonLabel: '搜索',
    createButtonLabel: '创建',
    editButtonLabel: '编辑',
    deleteButtonLabel: '删除',
    clearButtonLabel: '清空',
    uploadButtonLabel: '上传',
    operateTitle: '操作',
    disableTitle: '该页面仅在配置中心类型为raft模式下可用',
    editTitle: '编辑配置',
    deleteTitle: '删除配置',
    deleteConfirmLabel: '确认需要删除以下配置项: ',
    deleteAllConfirmLabel: '确认需要清空以下namespace和dataId中的所有配置项: ',
    addTitle: '新增配置',
    operationSuccess: '操作成功！',
    operationFailed: '操作失败',
    inputFilterPlaceholder: '请选择筛选条件',
    fieldFillingTips: '请将必要字段填充完整',
    uploadTitle: '上传配置',
    uploadFileButtonLabel: '上传文件',
  },
};

export default zhCn;
