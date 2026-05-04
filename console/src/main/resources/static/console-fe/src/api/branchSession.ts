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
 * Branch session query parameters
 */
export interface BranchSessionQueryParams {
  /** XID */
  xid?: string
  /** Branch ID */
  branchId?: string
  /** Application ID */
  applicationId?: string
  /** Branch status code */
  status?: number
  /** Transaction name */
  transactionName?: string
  /** Transaction group */
  vgroup?: string
  /** Namespace */
  namespace?: string
  /** Cluster */
  cluster?: string
}

/**
 * Branch session operation parameters (for mutation APIs)
 */
export interface BranchSessionOperationParams {
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
 * Delete a branch session
 */
export function deleteBranchSession(params: BranchSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, branchId, vgroup, namespace, cluster } = params
  return request.delete('/console/branchSession/deleteBranchSession', {
    params: { xid, branchId, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Force delete a branch session
 */
export function forceDeleteBranchSession(params: BranchSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, branchId, vgroup, namespace, cluster } = params
  return request.delete('/console/branchSession/forceDeleteBranchSession', {
    params: { xid, branchId, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Stop branch session retry
 */
export function stopBranchRetry(params: BranchSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, branchId, vgroup, namespace, cluster } = params
  return request.put('/console/branchSession/stopBranchSession', null, {
    params: { xid, branchId, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}

/**
 * Start branch session retry
 */
export function startBranchRetry(params: BranchSessionOperationParams): Promise<{ code: string; message: string }> {
  const { xid, branchId, vgroup, namespace, cluster } = params
  return request.put('/console/branchSession/startBranchSession', null, {
    params: { xid, branchId, vgroup },
    headers: {
      'x-seata-namespace': namespace,
      'x-seata-cluster': cluster,
    },
  }) as Promise<{ code: string; message: string }>
}
