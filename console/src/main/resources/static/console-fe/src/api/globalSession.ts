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
 * Global session operation parameters (for mutation APIs)
 */
export interface GlobalSessionOperationParams {
  /** XID */
  xid: string
  /** Transaction group */
  vgroup?: string
  /** Namespace */
  namespace?: string
  /** Cluster */
  cluster?: string
}

/**
 * Global session query parameters (mapped from backend GlobalSessionParam + BaseParam)
 */
export interface GlobalSessionQueryParams {
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
  /** Application ID */
  applicationId?: string
  /** Global transaction status code */
  status?: number
  /** Transaction name */
  transactionName?: string
  /** Transaction group */
  vgroup?: string
  /** Whether to include branch transactions */
  withBranch?: boolean
  /** Namespace */
  namespace?: string
  /** Cluster */
  cluster?: string
}

/**
 * Global session record (mapped from backend GlobalSessionVO)
 */
export interface GlobalSessionRecord {
  xid: string
  transactionId: string
  /** Status code */
  status: number
  applicationId: string
  transactionServiceGroup: string
  transactionName: string
  timeout: number
  /** Start time (millisecond timestamp) */
  beginTime: number
  applicationData: string
  /** Creation time (millisecond timestamp) */
  gmtCreate: number
  /** Modification time (millisecond timestamp) */
  gmtModified: number
  /** Branch session list (returned when withBranch=true) */
  branchSessionVOs?: unknown[]
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
 * Query global sessions with pagination
 */
export function queryGlobalSessions(params: GlobalSessionQueryParams): Promise<PageResponse<GlobalSessionRecord>> {
  const { namespace, cluster, ...queryParams } = params
  return request.get('/console/globalSession/query', {
    params: queryParams,
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<PageResponse<GlobalSessionRecord>>
}

/**
 * Delete a global session
 */
export function deleteGlobalSession(params: GlobalSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, vgroup, namespace, cluster } = params
  return request.delete('/console/globalSession/deleteGlobalSession', {
    params: { xid, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Force delete a global session
 */
export function forceDeleteGlobalSession(params: GlobalSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, vgroup, namespace, cluster } = params
  return request.delete('/console/globalSession/forceDeleteGlobalSession', {
    params: { xid, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Stop global session retry
 */
export function stopGlobalRetry(params: GlobalSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, vgroup, namespace, cluster } = params
  return request.put('/console/globalSession/stopGlobalSession', null, {
    params: { xid, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Start global session retry
 */
export function startGlobalRetry(params: GlobalSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, vgroup, namespace, cluster } = params
  return request.put('/console/globalSession/startGlobalSession', null, {
    params: { xid, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Send commit or rollback for a global transaction
 */
export function sendCommitOrRollback(params: GlobalSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, vgroup, namespace, cluster } = params
  return request.put('/console/globalSession/sendCommitOrRollback', null, {
    params: { xid, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Change global transaction status
 */
export function changeGlobalStatus(params: GlobalSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, vgroup, namespace, cluster } = params
  return request.put('/console/globalSession/changeGlobalStatus', null, {
    params: { xid, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Global status code mapping, aligned with {@link org.apache.seata.core.model.GlobalStatus}
 */
export const GlobalStatusMap: Record<number, string> = {
  0: 'UnKnown',
  1: 'Begin',
  2: 'Committing',
  3: 'CommitRetrying',
  4: 'Rollbacking',
  5: 'RollbackRetrying',
  6: 'TimeoutRollbacking',
  7: 'TimeoutRollbackRetrying',
  8: 'AsyncCommitting',
  9: 'Committed',
  10: 'CommitFailed',
  11: 'Rollbacked',
  12: 'RollbackFailed',
  13: 'TimeoutRollbacked',
  14: 'TimeoutRollbackFailed',
  15: 'Finished',
  16: 'CommitRetryTimeout',
  17: 'RollbackRetryTimeout',
  18: 'Deleting',
  19: 'StopCommitOrCommitRetry',
  20: 'StopRollbackOrRollbackRetry',
}
