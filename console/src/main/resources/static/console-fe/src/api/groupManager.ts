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
import qs from 'qs'

/**
 * Add a transaction group
 */
export function addGroup(namespace: string, clusterName: string, vGroup: string, unitName: string = ''): Promise<any> {
  return request.post('/naming/addGroup', qs.stringify({ namespace, clusterName, vGroup, unitName }), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  }) as Promise<any>
}

/**
 * Change a transaction group
 */
export function changeGroup(namespace: string, clusterName: string, vGroup: string, unitName: string = ''): Promise<any> {
  return request.post('/naming/changeGroup', qs.stringify({ namespace, clusterName, vGroup, unitName }), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  }) as Promise<any>
}
