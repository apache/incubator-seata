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
  <el-container class="not-found-container">
    <el-result icon="404" :title="t('notFound.title')" :sub-title="t('notFound.description')">
      <template #extra>
        <el-button type="primary" size="large" @click="router.push('/')">
          <el-icon><House /></el-icon>
          {{ t('notFound.backHome') }}
        </el-button>
      </template>
    </el-result>
  </el-container>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { House } from '@element-plus/icons-vue'

const { t } = useI18n()
const router = useRouter()
</script>

<style lang="scss" scoped>
.not-found-container {
  height: 100vh;
  @include flex-center;
}
</style>
