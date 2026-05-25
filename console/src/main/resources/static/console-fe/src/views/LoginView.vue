<!--
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
-->
<template>
  <div class="login-page">
    <div class="top-section">
      <!-- Animated particles (positioned relative to the full area) -->
      <div class="animation animation1" />
      <div class="animation animation2" />
      <div class="animation animation3" />
      <div class="animation animation4" />
      <div class="animation animation5" />

      <!-- Content constraint container -->
      <div class="login-content">
        <!-- Left product area -->
        <div class="product-area">
          <img class="product-logo" src="@/assets/seata_logo_white.png" alt="Seata Logo" />
          <p class="product-desc">{{ t('login.description') }}</p>
        </div>

        <!-- Right login panel -->
        <div class="login-panel">
        <el-card :header="t('login.title')" shadow="always" class="login-card">
          <el-alert
            :title="t('login.warning')"
            type="warning"
            :closable="false"
            show-icon
            class="login-warning"
          />

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
            class="login-form"
            @submit.prevent="handleSubmit"
          >
            <el-form-item :label="t('login.username')" prop="username" label-width="100px">
              <el-input
                v-model="form.username"
                :placeholder="t('login.usernamePlaceholder')"
                clearable
              />
            </el-form-item>

            <el-form-item :label="t('login.password')" prop="password" label-width="100px">
              <el-input
                v-model="form.password"
                type="password"
                :placeholder="t('login.passwordPlaceholder')"
                show-password
              />
            </el-form-item>

            <el-form-item label-width="100px">
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
              >
                {{ t('login.submit') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { useAppStore } from '@/stores/app'

const { t } = useI18n()
const router = useRouter()
const appStore = useAppStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = reactive<FormRules>({
  username: [{ required: true, message: () => t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('login.passwordRequired'), trigger: 'blur' }],
})

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const username = form.username.trim()
    const result = await login({
      username,
      password: form.password,
    })

    appStore.setToken(result.data)
    appStore.setUsername(username)
    await router.push('/transaction/list')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
$animation-duration: 2s;

@keyframes slashStar {
  0% { opacity: 1; }
  100% { opacity: 0; }
}

.login-page {
  flex: 1;
  display: flex;
  flex-direction: column;

  .top-section {
    position: relative;
    flex: 1;
    min-height: 600px;
    overflow-x: auto;
    background: url('@/assets/black_dot.png') repeat;
    background-size: 14px 14px;

    /* Animated particles: full-width positioned relative to .top-section */
    .animation {
      position: absolute;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: #1be1f6;
    }
    .animation1 { left: 15%; top: 70%; animation: slashStar $animation-duration ease-in-out 0.3s infinite; }
    .animation2 { left: 34%; top: 35%; animation: slashStar $animation-duration ease-in-out 1.2s infinite; }
    .animation3 { left: 53%; top: 20%; animation: slashStar $animation-duration ease-in-out 0.5s infinite; }
    .animation4 { left: 72%; top: 64%; animation: slashStar $animation-duration ease-in-out 0.8s infinite; }
    .animation5 { left: 87%; top: 30%; animation: slashStar $animation-duration ease-in-out 1.5s infinite; }

    /* Content constraint container: absolute positioned to fill .top-section, centered with max-width */
    .login-content {
      position: absolute;
      inset: 0;
      max-width: 1600px;
      min-width: 1200px;
      margin: 0 auto;

      /* Left product area */
      .product-area {
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 600px;
        margin-left: 40px;
      }

      .product-logo {
        display: block;
        width: 257px;
        height: 50px;
        margin: 0;
      }

      .product-desc {
        opacity: 0.8;
        font-size: 24px;
        color: #fff;
        max-width: 780px;
        margin: 12px 0 30px;
        line-height: 32px;
      }

      /* Right login panel */
      .login-panel {
        position: absolute;
        right: 40px;
        width: 480px;
        top: 50%;
        transform: translateY(-50%);

        .login-card {
          :deep(.el-card__header) {
            font-size: 16px;
            font-weight: 600;
          }

          :deep(.el-card__body) {
            padding: 32px 40px;
          }
        }

        .login-warning {
          margin-bottom: 20px;
        }
      }
    }
  }
}
</style>

