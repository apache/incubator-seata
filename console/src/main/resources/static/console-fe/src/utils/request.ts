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
import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { Message } from '@alifd/next';
import { get } from 'lodash';
import { AUTHORIZATION_HEADER } from '@/contants';
import { getCurrentLocaleObj } from '@/reducers/locale';

const createRequest = (baseURL: string, generalErrorMessage: string = 'Request error, please try again later!') => {
  const instance: AxiosInstance = axios.create({
    baseURL,
    method: 'get',
  });

  instance.interceptors.request.use((config: any) => {
    let authHeader: string | null = localStorage.getItem(AUTHORIZATION_HEADER);
    // add jwt header
    if (config.headers) {
      config.headers[AUTHORIZATION_HEADER] = authHeader;
    }
    return config;
  });

  instance.interceptors.response.use(
    (response: AxiosResponse): Promise<any> => {
      const code = get(response, 'data.code');
      if (response.status === 200 && code === '200') {
        return Promise.resolve(get(response, 'data'));
      } else {
        const currentLocale = getCurrentLocaleObj();
        const errorText =
          (currentLocale.codeMessage as any)[code] ||
          get(response, 'data.message') ||
          get(response, 'data.errorMsg') ||
          response.statusText;
        Message.error(errorText || `Request error ${code}: ${get(response, 'config.url', '')}`);
        return Promise.reject(response);
      }
    },
    error => {
      if (error.response) {
        const { status } = error.response;
        if (status === 403 || status === 401) {
          (window as any).globalHistory.replace('/login');
          return;
        }
        Message.error(`HTTP ERROR: ${status}`);
      } else {
        Message.error(generalErrorMessage);
      }
      return Promise.reject(error);
    }
  );

  return instance;
};

const request = createRequest('api/v1');

export { createRequest };
export default request;
