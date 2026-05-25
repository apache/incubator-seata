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
import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css';
import i18n from '@/i18n'
import router from '@/router'
import { useAppStore } from '@/stores/app'

const AUTHORIZATION_HEADER = 'Authorization'
const DEFAULT_GENERAL_ERROR_MESSAGE = 'Request error, please try again later!'

type LocaleMessages = {
  codeMessage?: Record<string, string>
}

type ResponseBody = {
  code?: string | number
  message?: string
  errorMsg?: string
}

function getCurrentLocaleMessages(): LocaleMessages {
  const locale = i18n.global.locale.value
  return i18n.global.getLocaleMessage(locale) as LocaleMessages
}

function setAuthorizationHeader(config: InternalAxiosRequestConfig, token: string | null) {
  if (config.headers && typeof config.headers.set === 'function') {
    config.headers.set(AUTHORIZATION_HEADER, token ?? '')
    return
  }

  config.headers = {
    ...config.headers,
    [AUTHORIZATION_HEADER]: token,
  }
}

const createRequest = (
  baseURL: string,
  generalErrorMessage: string = DEFAULT_GENERAL_ERROR_MESSAGE,
): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    method: 'get',
  })

  instance.interceptors.request.use((config) => {
    const appStore = useAppStore()
    const authHeader = appStore.getToken()
    setAuthorizationHeader(config, authHeader)
    return config
  })

  instance.interceptors.response.use(
    (response: AxiosResponse<ResponseBody>) => {
      const code = response.data?.code

      if (response.status === 200 && String(code) === '200') {
        return Promise.resolve(response.data)
      }

      const currentLocale = getCurrentLocaleMessages()
      const errorText =
        currentLocale.codeMessage?.[String(code)] ||
        response.data?.message ||
        response.data?.errorMsg ||
        response.statusText

      ElMessage.error(errorText || `Request error ${code}: ${response.config.url ?? ''}`)
      return Promise.reject(response)
    },
    (error: AxiosError<ResponseBody>) => {
      if (error.response) {
        const { status } = error.response

        if (status === 403 || status === 401) {
          window.location.replace('/login')
          return Promise.reject(error)
        }
        ElMessage.error(`HTTP ERROR: ${status}`)
      } else {
        ElMessage.error(generalErrorMessage)
      }

      return Promise.reject(error)
    },
  )

  return instance
}

const request = createRequest('/api/v1')

export { AUTHORIZATION_HEADER, createRequest }
export default request
