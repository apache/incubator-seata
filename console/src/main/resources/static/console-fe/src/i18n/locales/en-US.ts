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
    '401': 'Login expired. Please sign in again.',
    '403': 'You do not have permission to perform this action.',
    '404': 'The requested resource was not found.',
    '500': 'Service error. Please try again later.',
  },
  nav: {
    transactionList: 'Transactions',
    globalLockList: 'Global Locks',
    sagaStateMachineDesigner: 'Saga State Machine Designer',
    clusterManager: 'Cluster Manager',
    groupManager: 'Group Manager',
  },
  common: {
    confirm: 'Confirm',
    cancel: 'Cancel',
    save: 'Save',
    delete: 'Delete',
    edit: 'Edit',
    reset: 'Reset',
    logout: 'Logout',
    defaultUser: 'User',
    operation: 'Operation',
    search: 'Search',
    loading: 'Loading...',
    noData: 'No Data',
    success: 'Success',
    error: 'Error',
    close: 'Close',
    more: 'More',
  },
  transaction: {
    title: 'Transactions',
    subTitle: 'Basic List',
    filters: {
      createTime: 'Create Time',
      to: '-',
      startDate: 'Start Date',
      endDate: 'End Date',
      inputPlaceholder: 'Please enter filter text',
      selectPlaceholder: 'Please select a condition',
      selectTransactionGroup: 'Please select transaction group',
      includeBranch: 'Include branch transactions',
    },
    table: {
      actions: 'Actions',
    },
    actions: {
      detail: 'View Global Lock',
      deleteGlobal: 'Delete Global Transaction',
      forceDeleteGlobal: 'Force Delete Global Transaction',
      stopRetry: 'Stop Global Retry',
      startRetry: 'Start Global Retry',
      submitRetryGlobal: 'Commit or Rollback Global Transaction',
      updateStatus: 'Update Global Transaction Status',
      more: 'More',
      viewBranch: 'View Branch Sessions',
    },
  },
  globalLock: {
    title: 'Global Locks',
    subTitle: 'Basic List',
    filters: {
      createTime: 'Create Time',
      to: '-',
      startDate: 'Start Date',
      endDate: 'End Date',
      inputPlaceholder: 'Please enter filter text',
      selectTransactionGroup: 'Please select transaction group',
    },
    table: {
      actions: 'Actions',
    },
    actions: {
      delete: 'Delete Global Lock',
    },
    warning: {
      deleteWarning: 'Dirty write problem exists. The branch transactions may be affected. Are you sure you want to delete the global lock?',
    },
  },
  branch: {
    title: 'Branch Sessions',
    table: {
      transactionId: 'Transaction ID',
      branchId: 'Branch ID',
      resourceGroupId: 'Resource Group ID',
      branchType: 'Branch Type',
      status: 'Status',
      resourceId: 'Resource ID',
      clientId: 'Client ID',
      applicationData: 'Application Data',
      actions: 'Actions',
    },
    actions: {
      delete: 'Delete Branch',
      forceDelete: 'Force Delete Branch',
      stopRetry: 'Stop Retry',
      startRetry: 'Start Retry',
    },
  },
  notFound: {
    title: 'Page Not Found',
    description: 'The page you are looking for does not exist.',
    backHome: 'Back to Home',
  },
  publicNav: {
    home: 'Home',
    docs: 'Docs',
    blog: 'Blog',
    community: 'Community',
    download: 'Download',
  },
  sagaStateMachineDesigner: {
    title: 'Saga State Machine Designer',
  },
  clusterManager: {
    title: 'Cluster Manager',
    subTitle: 'Manage Clusters',
    unitName: 'Unit Name',
    members: 'Members',
    clusterType: 'Cluster Type',
    operations: 'Operations',
    view: 'View',
    unitDialogTitle: 'Unit',
    control: 'Control',
    transaction: 'Transaction',
    internal: 'Internal',
    weight: 'Weight',
    healthy: 'Healthy',
    term: 'Term',
    role: 'Role',
    unit: 'Unit',
    version: 'Version',
    metadata: 'Metadata',
    metadataDialogTitle: 'Metadata',
    controlEndpoint: 'Control Endpoint',
    transactionEndpoint: 'Transaction Endpoint',
    selectNamespacePlaceholder: 'Select namespace',
    selectClusterPlaceholder: 'Select cluster',
  },
  groupManager: {
    title: 'Group Manager',
    subTitle: 'Manage Groups',
    table: {
      namespace: 'Namespace',
      cluster: 'Cluster',
      vgroup: 'VGroup',
      clusterType: 'Cluster Type',
      units: 'Unit',
      actions: 'Actions',
    },
    create: {
      button: 'Create VGroup',
      dialogTitle: 'Create VGroup',
      vgroupName: 'VGroup Name',
      vgroupNamePlaceholder: 'Enter VGroup name',
      confirm: 'Create',
      success: 'VGroup created successfully',
      fail: 'Failed to create vgroup',
      errorMessage: 'Please select namespace, cluster and enter vgroup name',
    },
    change: {
      button: 'Change VGroup',
      dialogTitle: 'Change VGroup',
      originalNamespace: 'Original Namespace',
      originalCluster: 'Original Cluster',
      selectVGroup: 'Select VGroup',
      originalUnit: 'Original Unit',
      targetNamespace: 'Target Namespace',
      targetCluster: 'Target Cluster',
      targetUnit: 'Target Unit',
      targetSection: 'Target Cluster',
      confirm: 'Confirm',
      confirmMessage: 'Are you sure you want to migrate the transaction group to the target cluster? This operation cannot be undone.',
      success: 'VGroup changed successfully',
      fail: 'Failed to change vgroup',
    },
  },
  login: {
    title: 'Login',
    username: 'Username',
    password: 'Password',
    usernamePlaceholder: 'Please enter username',
    passwordPlaceholder: 'Please enter password',
    submit: 'Login',
    warning: 'Warning: Internal system, do not expose to the public network',
    usernameRequired: 'Please enter username',
    passwordRequired: 'Please enter password',
    description: 'Seata is an open source distributed transaction solution that delivers high performance and easy to use distributed transaction services under a microservices architecture.',
  },
}
