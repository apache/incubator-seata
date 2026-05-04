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
  <header class="public-header">
    <div class="header-inner">
      <!-- Logo -->
      <div class="header-logo">
        <a :href="`${sitePrefix}/`" target="_blank" rel="noopener noreferrer">
          <img src="@/assets/seata_logo.png" alt="Seata Logo" class="logo-img" />
        </a>
      </div>

      <!-- Navigation links -->
      <nav class="header-nav">
        <a
          v-for="item in navItems"
          :key="item.key"
          :href="item.href"
          target="_blank"
          rel="noopener noreferrer"
          class="nav-link"
        >
          {{ t(`publicNav.${item.key}`) }}
        </a>
      </nav>

      <div class="header-actions">
        <!-- Language switcher -->
        <div class="header-lang">
          <span
            class="lang-item"
            :class="{ active: appStore.locale === 'zh-CN' }"
            @click="appStore.setLocale('zh-CN')"
          >中</span>
          <span class="lang-divider">|</span>
          <span
            class="lang-item"
            :class="{ active: appStore.locale === 'en-US' }"
            @click="appStore.setLocale('en-US')"
          >EN</span>
        </div>

        <el-dropdown
          v-if="!isLoginPage"
          trigger="click"
          class="user-dropdown"
          @command="handleUserCommand"
        >
          <div class="user-trigger">
            <el-tooltip :content="displayUsername" placement="left" effect="dark">
              <el-avatar :size="30" class="user-avatar">
                {{ avatarText }}
              </el-avatar>
            </el-tooltip>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">{{ t('common.logout') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const { t } = useI18n()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

const isLoginPage = computed(() => route.path === '/login')
const displayUsername = computed(() => appStore.username || t('common.defaultUser'))
const avatarText = computed(() => displayUsername.value.trim().charAt(0).toUpperCase())

function handleUserCommand(command: string | number | object) {
  if (command !== 'logout') return
  appStore.clearToken()
  appStore.clearUsername()
  router.push('/login')
}

const SITE_BASE = 'https://seata.apache.org'

const sitePrefix = computed(() =>
  appStore.locale === 'zh-CN' ? `${SITE_BASE}/zh-cn` : SITE_BASE,
)

const navItems = computed(() => {
  const prefix = sitePrefix.value
  return [
    { key: 'home', href: `${prefix}/` },
    { key: 'docs', href: `${prefix}/docs/overview/what-is-seata/` },
    { key: 'blog', href: `${prefix}/blog` },
    { key: 'community', href: `${prefix}/community` },
    { key: 'download', href: `${prefix}/download/seata-server` },
  ]
})
</script>

<style lang="scss" scoped>
.public-header {
  height: 64px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid $color-border;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-inner {
    height: 100%;
    padding: 0 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .header-logo {
    flex-shrink: 0;

    .logo-img {
      width: 96px;
      display: block;
    }
  }

  .header-nav {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 0;
    margin-right: 16px;

    .nav-link {
      color: #333;
      text-decoration: none;
      font-size: 14px;
      padding: 0 20px;
      opacity: 0.6;
      transition: opacity 0.2s;
      white-space: nowrap;

      &:hover {
        opacity: 1;
      }
    }
  }

  .header-actions {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 12px;

    .header-lang {
      display: flex;
      align-items: center;
      gap: 4px;

      .lang-item {
        color: #333;
        font-size: 14px;
        cursor: pointer;
        opacity: 0.6;
        padding: 4px 6px;
        border-radius: 4px;
        transition: opacity 0.2s;

        &:hover,
        &.active {
          opacity: 1;
        }
      }

      .lang-divider {
        color: #ccc;
        font-size: 12px;
      }
    }

    .user-dropdown {
      display: inline-flex;
    }

    .user-trigger {
      display: inline-flex;
      align-items: center;
      cursor: pointer;
      outline: none;
    }

    .user-avatar {
      background: #0ea5e9;
      color: #fff;
      font-weight: 600;
    }
  }
}
</style>
