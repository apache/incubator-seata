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
import request from '@/utils/request'

/**
 * Global lock operation parameters (for mutation APIs)
 */
export interface GlobalLockOperationParams {
  /** XID */
  xid: string
  /** Branch ID */
  branchId: string
  /** Transaction group */
  vgroup?: string
  /** Namespace */
  namespace?: string
  /** Cluster */
  cluster?: string
}

/**
 * Global lock query parameters (mapped from backend GlobalLockParam + BaseParam)
 */
export interface GlobalLockQueryParams {
  /** Page number */
  pageNum?: number
  /** Items per page */
  pageSize?: number
  /** Start time (millisecond timestamp) */
  timeStart?: number | null
  /** End time (millisecond timestamp) */
  timeEnd?: number | null
  /** XID */
  xid?: string
  /** Table name */
  tableName?: string
  /** Transaction ID */
  transactionId?: string
  /** Branch ID */
  branchId?: string
  /** Primary key */
  pk?: string
  /** Resource ID */
  resourceId?: string
  /** Namespace */
  namespace?: string
  /** Cluster */
  cluster?: string
  /** Transaction group */
  vgroup?: string
}

/**
 * Global lock record (mapped from backend GlobalLockVO)
 */
export interface GlobalLockRecord {
  xid: string
  transactionId: string
  branchId: string
  resourceId: string
  tableName: string
  pk: string
  rowKey: string
  /** VGroup, backend VO field */
  vgroup?: string
  /** Creation time (millisecond timestamp) */
  gmtCreate: number
  /** Modification time (millisecond timestamp) */
  gmtModified: number
}

/**
 * Paginated response (mapped from backend PageResult)
 */
export interface PageResponse<T> {
  code: string
  message: string
  data: T[]
  total: number
  pages: number
  pageNum: number
  pageSize: number
}

/**
 * Query global locks with pagination
 */
export function queryGlobalLocks(params: GlobalLockQueryParams): Promise<PageResponse<GlobalLockRecord>> {
  const { namespace, cluster, ...queryParams } = params
  return request.get('/console/globalLock/query', {
    params: queryParams,
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<PageResponse<GlobalLockRecord>>
}

/**
 * Delete a global lock
 */
export function deleteGlobalLock(params: GlobalLockOperationParams): Promise<{ code: string; message: string }> {
  const { xid, branchId, vgroup, namespace, cluster } = params
  return request.delete('/console/globalLock/delete', {
    params: { xid, branchId, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Check if a global lock exists for the given branch session
 */
export function checkGlobalLock(params: GlobalLockOperationParams): Promise<{ code: string; message: string; data: boolean }> {
  const { xid, branchId, vgroup, namespace, cluster } = params
  return request.get('/console/globalLock/check', {
    params: { xid, branchId, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string; data: boolean }>
}
