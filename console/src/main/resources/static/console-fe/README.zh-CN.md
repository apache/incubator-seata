# seata-console (console-fe)

English version: [README.md](./README.md)

基于 Vue3 最新技术栈构建的前端工程基座，提供路由懒加载、Element Plus 按需导入、中英双语、SCSS 变量体系、ESLint/Prettier 规范、Husky 提交前校验等完整工程化能力。

---

## 目录

- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [可用脚本](#可用脚本)
- [开发服务器配置](#开发服务器配置)
- [架构设计](#架构设计)
  - [路由与懒加载](#路由与懒加载)
  - [状态管理](#状态管理)
  - [国际化](#国际化)
  - [Element Plus 按需导入](#element-plus-按需导入)
  - [SCSS 样式体系](#scss-样式体系)
- [配置文件说明](#配置文件说明)
- [开发规范](#开发规范)
- [代码格式化与 ESLint 检查](#代码格式化与-eslint-检查)
- [登录页实现说明](#登录页实现说明)
- [DefaultLayout 实现说明](#defaultlayout-实现说明)
- [TransactionListView 实现复盘（经验沉淀）](#transactionlistview-实现复盘经验沉淀)

---

## 技术栈

| 技术                                                      | 版本  | 说明                                    |
| --------------------------------------------------------- | ----- | --------------------------------------- |
| [Vue](https://vuejs.org/)                                 | ^3.5  | 渐进式 JavaScript 框架，Composition API |
| [Vite](https://vite.dev/)                                 | ^6.x  | 基于 ESM 的下一代前端构建工具           |
| [TypeScript](https://www.typescriptlang.org/)             | ^5.x  | 静态类型系统                            |
| [Vue Router](https://router.vuejs.org/)                   | ^4.x  | 官方路由，全部页面采用懒加载            |
| [Pinia](https://pinia.vuejs.org/)                         | ^2.x  | 轻量、类型安全的状态管理库              |
| [Vue I18n](https://vue-i18n.intlify.dev/)                 | ^9.x  | 国际化方案，支持中文 / English          |
| [Element Plus](https://element-plus.org/)                 | ^2.x  | 企业级 UI 组件库（自动按需导入）        |
| [SCSS](https://sass-lang.com/)                            | ^1.x  | CSS 预处理器，变量与混入全局注入        |
| [ESLint](https://eslint.org/)                             | ^9.x  | Flat Config 代码质量检查                |
| [Prettier](https://prettier.io/)                          | ^3.x  | 代码格式化                              |
| [Husky](https://typicode.github.io/husky/)                | ^9.x  | Git Hooks 管理                          |
| [lint-staged](https://github.com/lint-staged/lint-staged) | ^15.x | 仅对暂存文件执行校验                    |

---

## 目录结构

```text
console-fe/
├── build/                     # 构建脚本
│   ├── copy-dist.js           # 构建后递归将 dist/ 复制到静态资源目录
│   ├── copyDesigner.js        # 构建前将 saga-statemachine-designer 复制到 public/
│   └── version-plugin.js      # 原 Webpack 插件历史文件（功能已由 vite.config.ts 接管）
├── public/                    # 不参与构建的静态资源
├── src/
│   ├── assets/                # 参与构建的静态资源（图片、字体等）
│   ├── components/            # 通用业务组件（跨页面复用）
│   ├── i18n/                  # 国际化
│   │   ├── index.ts           # i18n 实例，读取 localStorage 初始化语言
│   │   └── locales/
│   │       ├── zh-CN.ts       # 中文词条
│   │       └── en-US.ts       # 英文词条
│   ├── layouts/               # 页面布局组件
│   │   └── DefaultLayout.vue  # 默认布局：侧边栏 + 顶部栏 + 内容区
│   ├── router/
│   │   └── index.ts           # 路由定义，所有页面均为动态 import 懒加载
│   ├── stores/                # Pinia 状态（Composition Store 风格）
│   │   ├── app.ts             # 全局应用状态（语言切换，持久化到 localStorage）
│   │   └── counter.ts         # 计数器演示状态
│   ├── styles/                # 全局 SCSS 样式体系
│   │   ├── variables.scss     # 设计令牌：颜色、间距、圆角、字体、阴影等
│   │   ├── mixins.scss        # 通用混入：flex 布局、文字截断、滚动条等
│   │   ├── reset.scss         # 纯 CSS 重置（无 SCSS 变量依赖）
│   │   └── global.scss        # 全局样式入口：@use reset + 主题基础样式 + 工具类
│   ├── utils/                 # 工具函数
│   ├── api/                   # API 请求封装
│   ├── views/                 # 路由页面组件（懒加载目标）
│   │   ├── HomeView.vue       # 首页：语言切换、计数器、技术栈展示
│   │   ├── AboutView.vue      # 关于页：技术栈表格
│   │   └── NotFoundView.vue   # 404 页面
│   ├── App.vue                # 根组件，集成 el-config-provider 同步 Element Plus 语言
│   ├── main.ts                # 应用入口：挂载 pinia、router、i18n，引入全局样式
│   ├── env.d.ts               # Vite 客户端类型 + .vue 文件类型声明
│   ├── auto-imports.d.ts      # unplugin-auto-import 自动生成（勿手动修改）
│   └── components.d.ts        # unplugin-vue-components 自动生成（勿手动修改）
├── .husky/
│   └── pre-commit             # 提交前钩子：执行 lint-staged
├── eslint.config.mjs          # ESLint Flat Config（Vue + TypeScript + Prettier）
├── .prettierrc.json           # Prettier 格式规则
├── .prettierignore            # Prettier 忽略列表
├── vite.config.ts             # Vite 配置（插件、别名、SCSS 全局注入）
├── tsconfig.json              # TS 项目引用入口
├── tsconfig.app.json          # src 源码 TS 配置（含路径别名）
├── tsconfig.node.json         # Node 侧 TS 配置（vite.config.ts）
└── package.json               # 依赖、scripts、engines、lint-staged 配置
```

---

## 快速开始

**环境要求**：Node.js >= 24

```bash
# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 构建生产包
npm run build

# 预览生产包
npm run preview
```

---

## 可用脚本

| 命令                 | 说明                                                                                                     |
| -------------------- | -------------------------------------------------------------------------------------------------------- |
| `npm run dev`        | 启动 Vite 开发服务器，HMR 热更新                                                                         |
| `npm run build`      | 复制 designer 资源、执行 TypeScript 类型检查、构建生产包，然后将 dist 复制到静态资源目录（输出到 dist/） |
| `npm run preview`    | 本地预览 dist/ 生产包                                                                                    |
| `npm run type-check` | 仅执行 TypeScript 类型检查，不产出文件                                                                   |
| `npm run lint`       | 对 .vue/.ts/.tsx 执行 ESLint 检查                                                                        |
| `npm run lint:fix`   | ESLint 检查并自动修复可修复问题                                                                          |
| `npm run format`     | Prettier 格式化 src/ 下所有 .vue/.ts/.tsx/.scss/.css/.json                                               |

---

## 开发服务器配置

Vite 开发服务器配置在 `vite.config.ts` 的 `server` 字段中：

| 配置项   | 值                              | 说明                                 |
| -------- | ------------------------------- | ------------------------------------ |
| 默认端口 | `5173`                          | 可通过 `--port` 参数修改             |
| 主机     | `localhost`                     | 可通过 `--host 0.0.0.0` 开放网络访问 |
| API 代理 | `/api` → `http://43.142.148.30` | 开发时自动代理 API 请求，解决跨域    |
| HMR      | WebSocket                       | 代码修改后自动热更新                 |

**常用启动变体**：

```bash
# 局域网内可访问（其他设备通过本机 IP 访问）
npm run dev -- --host 0.0.0.0

# 指定端口
npm run dev -- --port 3000

# 同时指定主机和端口
npm run dev -- --host 0.0.0.0 --port 3000
```

> **API 代理说明**：`/api` 开头的请求在开发阶段会被 Vite 代理转发到 `http://43.142.148.30`，避免浏览器跨域限制。生产部署时由后端网关或 Nginx 处理反向代理。

---

## 架构设计

### 路由与懒加载

所有路由页面组件统一使用动态 `import()` 实现懒加载，每个页面在首次访问时才加载对应的 JS chunk，减小首屏体积。

```ts
// src/router/index.ts
const routes = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/HomeView.vue'), // ← 懒加载
      },
    ],
  },
];
```

> **禁止**在 `router/index.ts` 中静态导入页面组件（如 `import HomeView from '@/views/HomeView.vue'`）。

---

### 状态管理

使用 Pinia **Composition Store** 风格（`defineStore` + `setup` 函数），与 Vue3 Composition API 保持一致。

```ts
// src/stores/app.ts（语言状态示例）
export const useAppStore = defineStore('app', () => {
  const locale = ref<'zh-CN' | 'en-US'>(localStorage.getItem('locale') ?? 'zh-CN');

  function setLocale(lang: 'zh-CN' | 'en-US') {
    locale.value = lang;
    i18n.global.locale.value = lang;
    localStorage.setItem('locale', lang); // 持久化
  }

  return { locale, setLocale };
});
```

新增业务 Store 在 `src/stores/` 下创建对应 `.ts` 文件，按模块拆分（如 `user.ts`、`transaction.ts`）。

---

### 国际化

通过 Vue I18n 9（Composition API 模式，`legacy: false`）管理中英文词条。语言状态由 `app store` 统一管理并持久化到 `localStorage`，页面刷新后自动恢复。

**添加词条**：在 `src/i18n/locales/zh-CN.ts` 和 `src/i18n/locales/en-US.ts` 中同步维护，结构保持一致。

```ts
// zh-CN.ts
export default {
  common: { confirm: '确认', cancel: '取消' },
};

// en-US.ts
export default {
  common: { confirm: 'Confirm', cancel: 'Cancel' },
};
```

**模板使用**：

```vue
<script setup lang="ts">
const { t } = useI18n();
</script>
<template>
  <el-button>{{ t('common.confirm') }}</el-button>
</template>
```

**切换语言**：

```ts
const appStore = useAppStore();
appStore.setLocale('en-US'); // 或 'zh-CN'
```

> **规范**：禁止在模板或逻辑代码中硬编码中文或英文字符串，统一通过 `t('key')` 取值。

---

### Element Plus 按需导入

通过 `unplugin-auto-import` + `unplugin-vue-components` 在构建时自动分析实际使用的组件和 API，只打包用到的部分。

```ts
// vite.config.ts（关键配置）
AutoImport({
  resolvers: [ElementPlusResolver()],
  imports: ['vue', 'vue-router', 'pinia', 'vue-i18n'], // 同时自动导入框架 API
});
Components({ resolvers: [ElementPlusResolver()] });
```

**直接使用，无需任何 import**：

```vue
<template>
  <el-button type="primary">按钮</el-button>
  <el-table :data="list" />
</template>
<script setup lang="ts">
// 命令式 API 同样自动导入
ElMessage.success('操作成功');
ElNotification({ title: '提示', message: '内容' });
</script>
```

> `src/auto-imports.d.ts` 和 `src/components.d.ts` 由插件自动生成，**不要手动修改**，已加入 `.gitignore`。

---

### SCSS 样式体系

| 文件             | 职责       | 说明                                                  |
| ---------------- | ---------- | ----------------------------------------------------- |
| `variables.scss` | 设计令牌   | 颜色、间距、圆角、字体、阴影、布局尺寸等 SCSS 变量    |
| `mixins.scss`    | 通用混入   | flex 布局、文字截断、绝对居中、滚动条、响应式断点     |
| `reset.scss`     | 浏览器重置 | 纯 CSS，**不含任何 SCSS 变量**（Sass 模块隔离限制）   |
| `global.scss`    | 全局入口   | `@use reset` + 主题基础样式（body/a/heading）+ 工具类 |

**变量和混入通过 `vite.config.ts` 全局注入，组件内直接使用：**

```vue
<style lang="scss" scoped>
.card {
  padding: $spacing-md; // 直接使用变量，无需 @use
  border-radius: $border-radius-md;
  @include flex-between; // 直接使用混入
  @include scrollbar(6px);
}
</style>
```

> **注意**：`additionalData` 仅注入 Vite 直接处理的 SCSS 文件。通过 `@use` 引入的子模块（`reset.scss`）是独立 Sass 模块，不继承注入内容，故 `reset.scss` 不能引用 SCSS 变量。

---

## 配置文件说明

### `vite.config.ts`

| 配置项                                        | 作用                                                          |
| --------------------------------------------- | ------------------------------------------------------------- |
| `@vitejs/plugin-vue`                          | 支持 `.vue` 单文件组件编译                                    |
| `unplugin-auto-import`                        | 自动导入 Vue/Router/Pinia/I18n API 及 Element Plus 命令式 API |
| `unplugin-vue-components`                     | 自动注册 Element Plus 组件（按需，无全量注册）                |
| `resolve.alias['@']`                          | `@` 路径别名指向 `src/`                                       |
| `css.preprocessorOptions.scss.additionalData` | 全局注入 SCSS 变量和混入                                      |

### `eslint.config.mjs`（ESLint 9 Flat Config）

- `eslint-plugin-vue` `flat/recommended`：Vue3 模板与 `<script setup>` 规则
- `@typescript-eslint`：TS 类型感知检查规则
- `eslint-config-prettier`：关闭与 Prettier 冲突的格式规则，格式完全交由 Prettier 管理

### TypeScript 配置（项目引用模式）

| 文件                 | 作用                                     | 编译目标 |
| -------------------- | ---------------------------------------- | -------- |
| `tsconfig.json`      | 项目引用入口，不含 compilerOptions       | —        |
| `tsconfig.app.json`  | `src/` 源码 TS 配置（含 `@/*` 路径别名） | ES2020   |
| `tsconfig.node.json` | `vite.config.ts` 等 Node 侧文件          | ES2022   |

---

## 开发规范

### 1. 目录与组件归位

**视图层（路由直达页面）**：

- 路由入口组件必须在 `src/views/`，使用 `*View.vue` 命名。
- 页面内私有子块，优先放页面同级子目录（如 `src/views/home/components/`），避免污染全局 `components`。

**通用组件层（跨页面复用）**：当一个组件满足以下任意条件时，抽到 `src/components/`：

- 被 2 个及以上页面/布局复用。
- 具备稳定输入输出（props/emits）且业务语义清晰。
- 抽离后能显著减少重复模板/样式/逻辑。
- 仅单页面使用且强耦合业务语义的组件，不要过度上提到全局组件目录。

**布局层**：仅负责壳层结构（header/aside/main/footer）与路由承载，业务逻辑应下沉到 views 或业务组件。

### 2. 组件与文件命名

| 类型       | 位置                | 命名规则                       | 示例                                |
| ---------- | ------------------- | ------------------------------ | ----------------------------------- |
| 页面组件   | `src/views/`        | `*View.vue`（PascalCase）      | `TransactionListView.vue`           |
| 布局组件   | `src/layouts/`      | `*Layout.vue`（PascalCase）    | `DefaultLayout.vue`                 |
| 通用组件   | `src/components/`   | PascalCase，语义化前缀优先     | `AppHeader.vue`、`SeataTable.vue`   |
| Store      | `src/stores/`       | 小写业务名，导出 `useXxxStore` | `user.ts` → `useUserStore`          |
| 组合式函数 | `src/utils/` 或就近 | `useXxx.ts`                    | `usePagination.ts`                  |
| 事件       | —                   | 语义动词短语                   | `submit`、`confirm`、`changeLocale` |

### 3. 组件优先

优先使用 Element Plus 组件构建页面结构与交互，仅在无合适组件时使用原生标签：

```vue
<!-- ✅ 优先 -->
<el-card><el-table :data="list" /></el-card>

<!-- ❌ 避免（有 EP 替代时） -->
<div class="card"><table>...</table></div>
```

Element Plus 已通过 `unplugin-vue-components` 配置自动按需导入，模板中直接使用组件，无需手动 `import`。

### 4. 路由懒加载

```ts
// ✅ 正确
component: () => import('@/views/MyView.vue');

// ❌ 禁止
import MyView from '@/views/MyView.vue';
component: MyView;
```

路由定义统一在 `src/router/index.ts` 中管理。

### 5. 国际化

所有用户可见文案优先走 i18n，避免硬编码中英文：

```vue
<!-- ✅ 正确 -->
<el-text>{{ t('common.save') }}</el-text>

<!-- ❌ 禁止 -->
<el-text>保存</el-text>
```

新增文案需**同时维护** `src/i18n/locales/zh-CN.ts` 和 `src/i18n/locales/en-US.ts`，保持结构对齐。

### 6. 样式规范

```vue
<style lang="scss" scoped>
/* ✅ 直接使用全局注入变量和混入，无需 @use */
.wrapper {
  padding: $spacing-lg;
  @include flex-center;
}

/* ❌ 不需要手动 @use（已全局注入） */
@use '@/styles/variables.scss' as *;
</style>
```

**核心规则**：

- 默认使用 `<style lang="scss" scoped>`，仅在确有必要时使用 `:deep()` 穿透。
- SCSS 变量与混入来自 `src/styles/variables.scss`、`src/styles/mixins.scss`（已在 Vite 中全局注入），SFC 样式内**禁止手动 `@use`** 引入。
- 新增或重构区域优先向 BEM 靠拢：`block`、`block__element`、`block--modifier`；兼容现有语义化 kebab-case 写法。
- 状态类统一 `is-*` / `has-*`（如 `is-active`、`has-error`）。
- 避免超过 3 层嵌套选择器；深层结构优先拆组件。
- 避免无语义类名（如 `box1`、`left`、`red-text`）。
- 主题变量、间距、字号优先使用 SCSS 变量，不硬编码魔法值。
- 全局可复用工具类集中在 `src/styles/global.scss`；组件内部样式保持 scoped。

### 7. 类型规范

- 优先明确声明类型，避免 `any`
- `defineProps` / `defineEmits` 优先使用泛型写法：

```ts
// ✅ 正确
defineProps<{ title: string; count?: number }>();
defineEmits<{ submit: [value: string]; close: [] }>();

// ❌ 避免
defineProps({ title: { type: String, required: true } });
```

- 不要手动编辑自动生成文件：`src/auto-imports.d.ts`、`src/components.d.ts`

### 8. API 请求

- 使用 `axios` 封装 API 请求，统一放在 `src/api/` 目录。
- 统一错误处理和 Token 管理。

### 9. 代码注释语言

**所有代码注释必须使用英文**。禁止在 `.vue`、`.ts`、`.js`、`.scss` 等源文件中使用中文注释。i18n 语言包（`zh-CN.ts`）的翻译值内容不受此限制。

### 10. TableActionColumn 组件（硬性规则）

所有含操作列的 `<el-table>` 必须使用 `src/components/TableActionColumn.vue`，禁止手写重复按钮+下拉模板。

```typescript
import TableActionColumn, { type RowAction } from '@/components/TableActionColumn.vue';

const actions: RowAction[] = [
  { key: 'delete', labelKey: 'xxx.actions.delete', buttonType: 'danger' },
  { key: 'forceDelete', labelKey: 'xxx.actions.forceDelete' },
  { key: 'stopRetry', labelKey: 'xxx.actions.stopRetry' },
];
```

```html
<TableActionColumn :actions="actions" label="操作" @action="handleAction" />
```

- `maxPrimary` prop（默认 `2`）控制直显按钮数，其余自动折叠到"更多"下拉。
- 组件内部自动计算列宽，父组件无需传 `width`。
- 操作确认弹窗逻辑在父组件 `handleAction` 中实现。

### 11. 操作确认弹窗（硬性规则）

所有非查看类操作（删除、强制删除、停止重试、开始重试、提交/回滚、更新状态等），在发起 API 请求前必须通过 `ElMessageBox.confirm` 弹窗让用户二次确认。查看类操作（详情、查看分支事务）不需要弹窗。

```typescript
import { ElMessage, ElMessageBox } from 'element-plus';

const confirmAction = async (labelKey: string): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(t(labelKey), t('common.confirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
    return true;
  } catch {
    return false;
  }
};

if (!(await confirmAction('some.actionLabelKey'))) return;
// ...发起 API 请求
```

### 12. 页面布局规范

**全高链路**（登录/后台布局）：

- `html/body/#app` 高度可继承，每层设置 `height: 100%`。
- 关键 flex 容器补 `min-height: 0` 防止溢出。

**表格查询页**：

- 采用"编辑态 + 提交态"双状态模型（如 `formState` / `queryState`），查询与重置后页码回到第一页。
- 表格操作列 `fixed="right"`，常用动作直显（最多 2 个），低频动作折叠到"更多"下拉菜单。
- 查询表单具备响应式降级策略（桌面多列、窄屏单列）。

---

## 代码格式化与 ESLint 检查

### 手动执行

```bash
# ESLint 检查所有 .vue/.ts/.tsx 文件
npm run lint

# ESLint 检查并自动修复可修复问题
npm run lint:fix

# Prettier 格式化 src/ 下所有代码文件
npm run format

# TypeScript 类型检查
npm run type-check
```

### 提交前自动检查

提交时自动触发 `.husky/pre-commit` → `lint-staged`，仅处理**当前暂存文件**：

1. `eslint --fix` — 自动修复可修复的 ESLint 问题
2. `prettier --write` — 格式化代码风格

`lint-staged` 配置（`package.json` 中定义）：

```text
*.{vue,ts,tsx} → eslint --fix, prettier --write
```

若 ESLint 存在无法自动修复的错误，提交将被**阻止**，需手动修复后重新 `git add` 并提交。

### Prettier 格式规则（`.prettierrc.json`）

```json
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "printWidth": 100,
  "trailingComma": "es5",
  "endOfLine": "lf"
}
```

---

## 登录页实现说明

登录页采用独立布局 `PublicLayout`，与后台管理的 `DefaultLayout` 完全隔离。

### 布局结构

```text
PublicLayout (src/layouts/PublicLayout.vue)
├── AppHeader (src/components/AppHeader.vue)   ← 公共顶部导航
├── <router-view />                            ← 页面内容插槽
└── AppFooter (src/components/AppFooter.vue)   ← 公共底部版权栏
```

路由配置：

```ts
{
  path: '/login',
  component: () => import('@/layouts/PublicLayout.vue'),
  children: [
    { path: '', name: 'Login', component: () => import('@/views/LoginView.vue') },
  ],
}
```

### 高度撑满方案

全屏高度继承链需要每一层都正确设置，否则 `height: 100%` 将失效：

```text
html { height: 100% }
  └── body { height: 100% }
        └── #app { height: 100% }
              └── .public-layout { height: 100vh; display: flex; flex-direction: column }
                    └── .public-main { flex: 1; display: flex; flex-direction: column; min-height: 0 }
                          └── .login-page { flex: 1; display: flex; flex-direction: column }
                                └── .top-section { flex: 1; min-height: 600px }
```

**关键点**：

- `html`、`body`、`#app` 须在 `reset.scss` 中设置 `height: 100%`
- flex 子项无法通过 `height: 100%` 向上继承 flex 分配的尺寸，需将父容器改为 flex 列容器后，子项用 `flex: 1` 撑满
- `min-height: 0` 防止 flex 子项内容溢出时无法收缩

### 宽屏约束与窄屏滚动

`.top-section` 使用 `overflow-x: auto`，内部 `.login-content` 通过绝对定位 + `max-width` + `margin: 0 auto` 实现居中约束：

```scss
.top-section {
  position: relative;
  overflow-x: auto;

  .login-content {
    position: absolute;
    inset: 0; // 等价于 top/right/bottom/left: 0
    max-width: 1600px; // 宽屏时居中约束，防止左右间距过大
    min-width: 1200px; // 窄屏时保持最小宽度，不压缩内容
    margin: 0 auto;
  }
}
```

> **注意**：`position: absolute` 同时设置 `inset: 0` 时，浏览器将元素宽度强制撑满父容器，`max-width` + `margin: 0 auto` 可正常生效。若改用 `left: 50%; transform: translateX(-50%)` 方案，则需改为 `width: 100%; max-width: ...`，两种方案不可混用。

### CSS 类命名约定

| 类名             | 说明                                    |
| ---------------- | --------------------------------------- |
| `.login-page`    | 登录页根容器，flex 列布局撑满父级       |
| `.top-section`   | 主内容区，黑点背景 + 粒子动画           |
| `.login-content` | 内容约束容器，绝对定位 + max-width 居中 |
| `.product-area`  | 左侧产品介绍区，绝对定位垂直居中        |
| `.login-panel`   | 右侧登录表单面板，绝对定位垂直居中      |
| `.login-card`    | 登录卡片，覆盖 Element Plus 默认内边距  |
| `.login-warning` | 警告提示，设置底部间距                  |

### i18n 命名空间约定

| 命名空间      | 用途                                             |
| ------------- | ------------------------------------------------ |
| `publicNav.*` | 顶部导航链接文字（HOME / DOCS / BLOG 等）        |
| `login.*`     | 登录页所有文案（标题、字段标签、提示、校验信息） |

### Logo 资源

| 文件                              | 来源                                              | 用途                   |
| --------------------------------- | ------------------------------------------------- | ---------------------- |
| `src/assets/seata_logo.png`       | https://seata.apache.org/img/seata_logo.png       | AppHeader 导航栏       |
| `src/assets/seata_logo_white.png` | https://seata.apache.org/img/seata_logo_white.png | 登录页左侧深色背景区域 |

---

## DefaultLayout 实现说明

`DefaultLayout` 是登录后的后台管理布局，包含固定顶部栏、左侧侧边栏菜单和可滚动内容区。

### 布局结构

```text
DefaultLayout (src/layouts/DefaultLayout.vue)
├── AppHeader                         ← 固定顶部导航栏
├── .layout-body (flex row)
│   ├── .layout-aside (240px)         ← 侧边栏
│   │   └── AppSidebarMenu            ← 菜单组件（撑满高度）
│   └── .layout-main (flex: 1)        ← 主内容区（可滚动）
│         └── <router-view />         ← 页面插槽（带 fade 过渡）
└── AppFooter                         ← 固定底部版权栏
```

路由配置：

```ts
{
  path: '/',
  component: () => import('@/layouts/DefaultLayout.vue'),
  children: [
    { path: '', name: 'Home', component: () => import('@/views/HomeView.vue') },
    { path: 'about', name: 'About', component: () => import('@/views/AboutView.vue') },
    { path: 'transaction/list', name: 'TransactionList', component: () => import('@/views/TransactionListView.vue') },
  ],
}
```

### Flex 高度撑满方案

与登录页相同，全屏高度继承须每层正确配置：

```text
.layout-container { height: 100vh; display: flex; flex-direction: column; overflow: hidden }
  └── AppHeader                        ← flex-shrink: 0（固定高度）
  └── .layout-body { flex: 1; min-height: 0; display: flex; overflow: hidden }
        └── .layout-aside { display: flex; flex-direction: column; min-height: 0 }
              └── AppSidebarMenu { flex: 1; min-height: 0 }   ← 撑满侧边栏高度
                    └── el-menu { height: 100% }
        └── .layout-main { flex: 1; overflow-y: auto }        ← 内容区独立滚动
  └── AppFooter                        ← flex-shrink: 0（固定高度）
```

**关键点**：

- `.layout-body` 设置 `min-height: 0` 防止 flex 子项溢出父容器
- `AppSidebarMenu` 在父容器（`layout-aside`）中用 `flex: 1; min-height: 0` 撑满剩余高度
- `el-menu` 用 `height: 100%` 填充 `AppSidebarMenu` 的全部空间；Element Plus 默认有 `border-right`，若需去除需通过 `:deep(.el-menu) { border-right: none }` 覆盖
- `.layout-main` 独立设置 `overflow-y: auto`，使页面内容在侧边栏和头尾固定的情况下独立滚动

### 路由与菜单统一配置（`src/router/routes.ts`）

路由定义与侧边栏菜单数据合并为单一来源，通过 `meta` 字段控制菜单行为，避免菜单与路由各自维护：

```ts
export interface AppRouteMeta {
  titleKey?: string    // i18n key，菜单显示文字
  icon?: Component     // Element Plus 图标组件
  order?: number       // 菜单排序
  showInMenu?: boolean // 是否出现在侧边栏
}

// 从路由定义中派生菜单项，保证单一来源
export const defaultLayoutMenuItems: AppMenuItem[] = defaultLayoutChildren
  .filter((r) => r.meta?.showInMenu)
  .sort((a, b) => (a.meta?.order ?? 99) - (b.meta?.order ?? 99))
  .map((r) => ({ path: ..., titleKey: ..., icon: ..., order: ... }))
```

新增页面只需在 `defaultLayoutChildren` 中添加路由并设置 `meta.showInMenu: true`，侧边栏菜单自动更新。

### AppSidebarMenu 组件

接收 `items: AppMenuItem[]` prop，通过 `:router="true"` 将菜单项 `index` 作为路由路径，由 `el-menu` 内部处理跳转：

```ts
// 自动高亮当前路由（含子路由模糊匹配）
const activeMenu = computed(() => {
  // 精确匹配 → 最长前缀匹配 → 默认首项
});
```

> **注意**：`el-menu` 的 `:router="true"` 模式下，`index` 必须与路由 `path` 完全一致（含前导 `/`），子路由在 `routes.ts` 中定义为相对路径（无 `/`），`AppSidebarMenu` 中使用时需确认实际 `path` 格式。

### CSS 类命名约定

| 类名                | 说明                                              |
| ------------------- | ------------------------------------------------- |
| `.layout-container` | 根容器，100vh，flex 列布局                        |
| `.layout-body`      | 中间区域，flex 行布局（侧边栏 + 内容区）          |
| `.layout-aside`     | 侧边栏容器，固定宽度，flex 列布局                 |
| `.layout-main`      | 主内容区，flex:1，独立垂直滚动                    |
| `.layout-footer`    | 底部版权栏，白色背景 + 顶部阴影（与 Header 一致） |

---

## TransactionListView 实现复盘（经验沉淀）

`TransactionListView` 作为典型的“筛选 + 表格 + 分页 + 行内操作”页面，最终采用了“状态分层 + 响应式表单 + 可折叠操作列”的实现方式。

### 经验 1：筛选编辑态与查询提交态分离

- 使用 `formState` 承载表单实时输入，使用 `queryState` 承载已提交查询条件。
- 点击“查询”时再把 `formState` 拷贝到 `queryState`，避免输入过程实时触发表格过滤导致体验抖动。
- 每次触发查询后重置 `currentPage = 1`，避免“旧页码超出新结果范围”导致空白页。

### 经验 2：筛选区优先保证可读性与响应式

- 筛选表单使用 CSS Grid：`repeat(auto-fit, minmax(...))`，保证桌面端密度与窄屏可收敛。
- 日期范围选择器使用 `filter-item-span-2` 在宽屏占两列，窄屏回退单列，避免控件挤压。
- 操作按钮行单独占满整行（`grid-column: 1 / -1`），保证视觉收束与操作一致性。

### 经验 3：行操作采用“主操作 + 更多”分组

- 常用动作放主按钮，低频动作收敛到下拉菜单，降低横向拥挤。
- 操作列固定在右侧（`fixed="right"`），避免横向滚动时丢失关键操作入口。
- 长文案字段统一使用 `show-overflow-tooltip`，兼顾信息密度与可读性。

### 经验 4：本地演示数据也要按真实域模型建模

- 明确声明 `TransactionRow` 与 `QueryState`，保持字段语义与后续 API 对接一致。
- `handlePageChange` 预留钩子，先对齐前端交互，再平滑接入后端分页查询。

### TransactionListView 硬性规则（后续同类页面复用）

1. 页面文案必须走 i18n key，禁止新增硬编码中英文。
2. 查询页必须区分“编辑态”和“提交态”（如 `formState/queryState`）。
3. 查询和重置都必须将页码重置到第一页。
4. 操作列必须右侧固定，且低频动作折叠到“更多”菜单。
5. 复杂筛选区必须具备响应式降级策略（桌面多列，移动端单列）。
