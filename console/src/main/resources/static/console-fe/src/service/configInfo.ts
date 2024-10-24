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
import {configRequest} from '@/utils/request';


export async function getConfig(params: { namespace: string, dataId: string}): Promise<any> {
  const result = await configRequest('/config/getAll', {
    method: 'get',
    params,
  });
  return result;
}

export async function putConfig(params: { namespace: string, dataId: string, key: string, value: string}): Promise<any> {
  const result = await configRequest('/config/put', {
    method: 'post',
    params,
  });
  return result;
}

export async function deleteConfig(params: { namespace: string, dataId: string, key: string }): Promise<any> {
  const result = await configRequest('/config/delete', {
    method: 'delete',
    params,
  });
  return result;
}

export async function deleteAllConfig(params: { namespace: string, dataId: string}): Promise<any> {
  const result = await configRequest('/config/deleteAll', {
    method: 'delete',
    params,
  });
  return result;
}

export async function uploadConfig(formData: FormData): Promise<any> {
  const result = await configRequest('/config/upload', {
    method: 'post',
    data: formData,
  });
  return result;
}

export async function getClusterInfo(): Promise<any> {
  const result = await configRequest('/config/cluster', {
    method: 'get',
  });
  return result;
}

export async function getAllNamespaces(): Promise<any> {
  const result = await configRequest('/config/getNamespaces', {
    method: 'get',
  });
  return result;
}

export async function getAllDataIds(params: { namespace: string}): Promise<any> {
  const result = await configRequest('/config/getDataIds', {
    method: 'get',
    params,
  });
  return result;
}
