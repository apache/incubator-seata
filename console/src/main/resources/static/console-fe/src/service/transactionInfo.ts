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
import request from '@/utils/request';

export type GlobalSessionParam = {
  xid?: string,
  applicationId?: string,
  status?: number,
  transactionName?: string,
  withBranch: boolean,
  pageSize: number,
  pageNum: number,
  timeStart?: number,
  timeEnd?: number
};

export type BranchSessionParam = {
  xid?: string,
  branchId?: string,
  applicationId?: string,
  status?: number,
  transactionName?: string,
};

export default async function fetchData(params:GlobalSessionParam):Promise<any> {
  let result = await request('/console/globalSession/query', {
    method: 'get',
    params,
  });

  return result;
}

export async function deleteGlobalData(params: GlobalSessionParam): Promise<any> {
  const xid = params.xid
  let result = await request('/console/globalSession/deleteGlobalSession', {
    method: 'delete',
    params: {
      xid
    },
  });
  return result;
}

export async function forceDeleteGlobalData(params: GlobalSessionParam): Promise<any> {
  const xid = params.xid
  let result = await request('/console/globalSession/forceDeleteGlobalSession', {
    method: 'delete',
    params: {
      xid
    },
  });
  return result;
}

export async function stopGlobalData(params: GlobalSessionParam): Promise<any> {
  const xid = params.xid
  let result = await request('/console/globalSession/stopGlobalSession', {
    method: 'PUT',
    params: {
      xid
    },
  });
  return result;
}

export async function startGlobalData(params: GlobalSessionParam): Promise<any> {
  const xid = params.xid
  let result = await request('/console/globalSession/startGlobalSession', {
    method: 'PUT',
    params: {
      xid
    },
  });
  return result;
}

export async function sendGlobalCommitOrRollback(params: BranchSessionParam): Promise<any> {
  const xid = params.xid
  let result = await request('/console/globalSession/sendCommitOrRollback', {
    method: 'PUT',
    params: {
      xid
    },
  });
  return result;
}

export async function changeGlobalData(params: GlobalSessionParam): Promise<any> {
  const xid = params.xid
  let result = await request('/console/globalSession/changeGlobalStatus', {
    method: 'PUT',
    params: {
      xid
    },
  });
  return result;
}

export async function deleteBranchData(params: BranchSessionParam): Promise<any> {
  const xid = params.xid
  const branchId = params.branchId
  let result = await request('/console/branchSession/deleteBranchSession', {
    method: 'delete',
    params: {
      xid,
      branchId
    },
  });
  return result;
}

export async function forceDeleteBranchData(params: BranchSessionParam): Promise<any> {
  const xid = params.xid
  const branchId = params.branchId
  let result = await request('/console/branchSession/forceDeleteBranchSession', {
    method: 'delete',
    params: {
      xid,
      branchId
    },
  });
  return result;
}

export async function stopBranchData(params: BranchSessionParam): Promise<any> {
  const xid = params.xid
  const branchId = params.branchId
  let result = await request('/console/branchSession/stopBranchSession', {
    method: 'PUT',
    params: {
      xid,
      branchId
    },
  });
  return result;
}

export async function startBranchData(params: BranchSessionParam): Promise<any> {
  const xid = params.xid
  const branchId = params.branchId
  let result = await request('/console/branchSession/startBranchSession', {
    method: 'PUT',
    params: {
      xid,
      branchId
    },
  });
  return result;
}
