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
export default {
  codeMessage: {
    '401': '登录状态已失效，请重新登录',
    '403': '当前账号无权限执行该操作',
    '404': '请求的资源不存在',
    '500': '服务异常，请稍后重试',
  },
  nav: {
    transactionList: '事务信息',
    globalLockList: '全局锁信息',
    sagaStateMachineDesigner: 'Saga 状态机设计器',
    clusterManager: '集群管理',
    groupManager: '事务分组管理',
  },
  common: {
    confirm: '确认',
    cancel: '取消',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    reset: '重置',
    logout: '退出',
    defaultUser: '用户',
    operation: '操作',
    search: '搜索',
    loading: '加载中...',
    noData: '暂无数据',
    success: '操作成功',
    error: '操作失败',
    close: '关闭',
    more: '更多',
  },
  transaction: {
    title: '事务信息',
    subTitle: '基础列表页',
    filters: {
      createTime: '创建时间',
      to: '-',
      startDate: '起始日期',
      endDate: '结束日期',
      inputPlaceholder: '请输入筛选条件',
      selectPlaceholder: '请选择筛选条件',
      selectTransactionGroup: '请选择事务分组',
      includeBranch: '是否包含分支事务',
    },
    table: {
      actions: '操作',
    },
    actions: {
      detail: '查看全局锁',
      deleteGlobal: '删除全局事务',
      forceDeleteGlobal: '强制删除全局事务',
      stopRetry: '停止全局事务重试',
      startRetry: '开始全局事务重试',
      submitRetryGlobal: '提交或回滚全局事务',
      updateStatus: '更新全局事务状态',
      more: '更多',
      viewBranch: '查看分支事务',
    },
  },
  globalLock: {
    title: '全局锁信息',
    subTitle: '基础列表页',
    filters: {
      createTime: '创建时间',
      to: '-',
      startDate: '起始日期',
      endDate: '结束日期',
      inputPlaceholder: '请输入筛选条件',
      selectTransactionGroup: '请选择事务分组',
    },
    table: {
      actions: '操作',
    },
    actions: {
      delete: '删除全局锁',
    },
    warning: {
      deleteWarning: '存在脏写问题，分支事务可能受到影响，确定要删除全局锁吗？',
    },
  },
  branch: {
    title: '分支事务',
    table: {
      transactionId: '事务ID',
      branchId: '分支ID',
      resourceGroupId: '资源组ID',
      branchType: '分支类型',
      status: '状态',
      resourceId: '资源ID',
      clientId: '客户端ID',
      applicationData: '应用数据',
      actions: '操作',
    },
    actions: {
      delete: '删除分支',
      forceDelete: '强制删除分支',
      stopRetry: '停止重试',
      startRetry: '开始重试',
    },
  },
  notFound: {
    title: '页面不存在',
    description: '您访问的页面不存在，请检查地址是否正确。',
    backHome: '返回首页',
  },
  publicNav: {
    home: '首页',
    docs: '文档',
    blog: '博客',
    community: '社区',
    download: '下载',
  },
  sagaStateMachineDesigner: {
    title: 'Saga 状态机设计器',
  },
  clusterManager: {
    title: '集群管理',
    subTitle: '管理集群',
    unitName: 'Unit 名称',
    members: '成员数',
    clusterType: '集群类型',
    operations: '操作',
    view: '查看',
    unitDialogTitle: '单元',
    control: '控制节点',
    transaction: '事务节点',
    internal: '内部节点',
    weight: '权重',
    healthy: '健康状态',
    term: '任期',
    role: '角色',
    unit: '单位',
    version: '版本',
    metadata: '元数据',
    metadataDialogTitle: '元数据',
    controlEndpoint: '控制端点',
    transactionEndpoint: '事务端点',
    selectNamespacePlaceholder: '请选择命名空间',
    selectClusterPlaceholder: '请选择集群',
  },
  groupManager: {
    title: '事务分组管理',
    subTitle: '管理事务分组',
    table: {
      namespace: '命名空间',
      cluster: '集群',
      vgroup: '事务分组',
      clusterType: '集群类型',
      units: 'Unit',
      actions: '操作',
    },
    create: {
      button: '创建事务分组',
      dialogTitle: '创建事务分组',
      vgroupName: '事务分组名称',
      vgroupNamePlaceholder: '请输入事务分组名称',
      confirm: '创建',
      success: '事务分组创建成功',
      fail: '创建事务分组失败',
      errorMessage: '请选择命名空间、集群并输入事务分组名称',
    },
    change: {
      button: '迁移事务分组',
      dialogTitle: '迁移事务分组',
      originalNamespace: '原命名空间',
      originalCluster: '原集群',
      selectVGroup: '选择事务分组',
      originalUnit: '原单元',
      targetNamespace: '目标命名空间',
      targetCluster: '目标集群',
      targetUnit: '目标单元',
      targetSection: '目标集群',
      confirm: '确认迁移',
      confirmMessage: '确认将事务分组迁移到目标集群？此操作不可撤销。',
      success: '事务分组迁移成功',
      fail: '迁移事务分组失败',
    },
  },
  login: {
    title: '登录',
    username: '用户名',
    password: '密码',
    usernamePlaceholder: '请输入用户名',
    passwordPlaceholder: '请输入密码',
    submit: '登录',
    warning: '警告：内部系统，请勿暴露到公网',
    usernameRequired: '请输入用户名',
    passwordRequired: '请输入密码',
    description: 'Seata 是一款开源的分布式事务解决方案，致力于提供高性能和简单易用的分布式事务服务。',
  },
}
