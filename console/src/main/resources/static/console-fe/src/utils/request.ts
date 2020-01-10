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
import axios from 'axios';
import { Message } from '@alicloud/console-components';
import { get } from 'lodash';
// import { SUCCESS_RESULT_CODE } from '../constants';

const API_GENERAL_ERROR_MESSAGE = 'Request error, please try again later!';

const codeMessage = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '用户没有权限（令牌、用户名、密码错误）。',
  403: '用户得到授权，但是访问是被禁止的。',
  404: '发出的请求针对的是不存在的记录，服务器没有进行操作。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。',
  '-1000': '项目名称已存在, 请使用其他名称',
};

const request = () => {
  const instance = axios.create({
    baseURL: '/api/v1',
    method: 'get',
  });

  instance.interceptors.response.use(
    response => {
      const code = get(response, 'data.code');
      if (response.status === 200 && code === 200) {
        return Promise.resolve(get(response, 'data.data'));
      } else {
        const errorText =
          (codeMessage as any)[code] ||
          get(response, 'data.message') ||
          get(response, 'data.errorMsg') ||
          response.statusText;
        Message.error({
          title: `请求错误 ${code}: ${get(response, 'config.url', '')}`,
          content: errorText,
          duration: 6,
        });
        return Promise.reject(response);
      }
    },
    error => {
      if (error && (error.status === 403 || error.status === 401)) {
        // 跳转至login页
        // TODO: 用 react-router 重写，改造成本比较高，这里先hack
        const url = window.location.href;
        const [base_url] = url.split('#');
        (window as any).location = `${base_url}#/login`;
        return;
      }
      if (error.response) {
        const { status } = error.response;
        Message.error(`HTTP ERROR: ${status}`);
      } else {
        Message.error(API_GENERAL_ERROR_MESSAGE);
      }
      return Promise.reject(error);
    }
  );

  return instance;
};

export default request();
