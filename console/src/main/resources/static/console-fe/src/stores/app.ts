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
import { defineStore } from 'pinia'
import { ref } from 'vue'
import i18n from '@/i18n'

type LocaleType = 'zh-CN' | 'en-US'
const TOKEN_STORAGE_KEY = 'Authorization'

export const useAppStore = defineStore('app', () => {
  const locale = ref<LocaleType>((localStorage.getItem('locale') as LocaleType) || 'zh-CN')
  const username = ref(localStorage.getItem('username') || '')

  function setLocale(lang: LocaleType) {
    locale.value = lang
    i18n.global.locale.value = lang
    localStorage.setItem('locale', lang)
    document.documentElement.lang = lang
  }

  function setUsername(name: string) {
    username.value = name
    localStorage.setItem('username', name)
  }

  function clearUsername() {
    username.value = ''
    localStorage.removeItem('username')
  }

  function setToken(value: string) {
    localStorage.setItem(TOKEN_STORAGE_KEY, value)
  }

  function clearToken() {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
  }

  function getToken() {
    return localStorage.getItem(TOKEN_STORAGE_KEY) || ''
  }

  return {
    locale,
    username,
    setLocale,
    setUsername,
    clearUsername,
    setToken,
    clearToken,
    getToken,
  }
})
