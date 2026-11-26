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
import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { List, Lock, Monitor, Cloudy, FolderOpened } from '@element-plus/icons-vue'

export interface AppRouteMeta {
  title?: string
  titleKey?: string
  icon?: Component
  order?: number
  showInMenu?: boolean
}

export interface AppMenuItem {
  path: string
  titleKey: string
  icon?: Component
  order: number
}

const defaultLayoutChildren: RouteRecordRaw[] = [
  {
    path: 'transaction/list',
    name: 'TransactionList',
    component: () => import('@/views/TransactionListView.vue'),
    meta: {
      title: 'transactionList',
      titleKey: 'nav.transactionList',
      icon: List,
      order: 1,
      showInMenu: true,
    },
  },
  {
    path: 'globallock/list',
    name: 'GlobalLockList',
    component: () => import('@/views/GlobalLockListView.vue'),
    meta: {
      title: 'globalLockList',
      titleKey: 'nav.globalLockList',
      icon: Lock,
      order: 2,
      showInMenu: true,
    },
  },
  {
    path: 'sagastatemachinedesigner',
    name: 'SagaStateMachineDesigner',
    component: () => import('@/views/SagaStateMachineDesignerView.vue'),
    meta: {
      title: 'sagaStateMachineDesigner',
      titleKey: 'nav.sagaStateMachineDesigner',
      icon: Monitor,
      order: 3,
      showInMenu: true,
    },
  },
  {
    path: 'cluster/list',
    name: 'ClusterManager',
    component: () => import('@/views/ClusterManagerView.vue'),
    meta: {
      title: 'clusterManager',
      titleKey: 'nav.clusterManager',
      icon: Cloudy,
      order: 4,
      showInMenu: true,
    },
  },
  {
    path: 'group/list',
    name: 'GroupManager',
    component: () => import('@/views/GroupManagerView.vue'),
    meta: {
      title: 'groupManager',
      titleKey: 'nav.groupManager',
      icon: FolderOpened,
      order: 5,
      showInMenu: true,
    },
  },
]

export const appRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/transaction/list',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: defaultLayoutChildren,
  },
  {
    path: '/login',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/LoginView.vue'),
        meta: { title: 'login' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

function joinPaths(parentPath: string, childPath: string): string {
  if (!childPath) return parentPath || '/'
  if (childPath.startsWith('/')) return childPath
  if (parentPath === '/' || !parentPath) return `/${childPath}`
  return `${parentPath}/${childPath}`
}

function collectMenuItems(routes: RouteRecordRaw[], parentPath: string): AppMenuItem[] {
  const menuItems: AppMenuItem[] = []

  routes.forEach((route) => {
    const fullPath = joinPaths(parentPath, route.path)
    const currentPath = fullPath === '//' ? '/' : fullPath
    const meta = (route.meta || {}) as AppRouteMeta

    if (meta.showInMenu !== false && meta.titleKey) {
      menuItems.push({
        path: currentPath,
        titleKey: meta.titleKey,
        icon: meta.icon,
        order: meta.order ?? 999,
      })
    }

    if (route.children?.length) {
      menuItems.push(...collectMenuItems(route.children, currentPath))
    }
  })

  return menuItems
}

export const defaultLayoutMenuItems: AppMenuItem[] = collectMenuItems(defaultLayoutChildren, '/')
  .filter((item) => item.path !== '/login')
  .sort((a, b) => a.order - b.order)
