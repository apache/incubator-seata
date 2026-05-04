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
  <div class="layout-container">
    <AppHeader />

    <div class="layout-body">
      <aside class="layout-aside">
        <AppSidebarMenu :items="menuItems" style="flex: 1; min-height: 0;" />
      </aside>

      <main class="layout-main">
        <router-view v-slot="{ Component: ViewComponent, route: currentRoute }">
          <transition name="fade" mode="out-in">
            <component :is="ViewComponent" :key="currentRoute.path" />
          </transition>
        </router-view>
      </main>
    </div>

    <AppFooter class="layout-footer" />
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import AppSidebarMenu from '@/components/AppSidebarMenu.vue'
import { defaultLayoutMenuItems } from '@/router/routes'

const menuItems = defaultLayoutMenuItems
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  background-color: $color-bg-base;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.layout-aside {
  width: $sidebar-width;
  background-color: #ffffff;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  transition: $transition-base;
}

.layout-main {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
  @include scrollbar;
}

.layout-footer {
  border-top: 1px solid $color-border;
  flex-shrink: 0;
}

// Route transition animation
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
