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
  <el-menu
    style="height: 100%"
    :default-active="activeMenu"
    :router="true"
  >
    <el-menu-item v-for="item in items" :key="item.path" :index="item.path">
      <el-icon v-if="item.icon">
        <component :is="item.icon" />
      </el-icon>
      <span>{{ t(item.titleKey) }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { AppMenuItem } from '@/router/routes'

interface Props {
  items: AppMenuItem[]
}

const props = defineProps<Props>()

const route = useRoute()
const { t } = useI18n()

const activeMenu = computed(() => {
  if (props.items.some((item) => item.path === route.path)) {
    return route.path
  }

  const matched = [...props.items]
    .sort((a, b) => b.path.length - a.path.length)
    .find((item) => route.path.startsWith(item.path) && item.path !== '/')

  return matched?.path || '/'
})
</script>

<style lang="scss" scoped>
:deep(.el-menu) {
  flex: 1;
  height: 100%;
  overflow-y: auto;
}
</style>
