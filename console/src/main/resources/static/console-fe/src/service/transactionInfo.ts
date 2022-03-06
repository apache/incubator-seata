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

export default async function fetchData(params:GlobalSessionParam):Promise<any> {
  let result = await request('/console/globalSession/query', {
    method: 'get',
    params,
  });

  return result;
}
