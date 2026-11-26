# seata-console (console-fe)

Chinese version: [README.zh-CN.md](./README.zh-CN.md)

This project is a frontend foundation for Seata Console Web, built on the latest Vue 3 stack. It provides route lazy loading, on-demand Element Plus imports, bilingual support, a shared SCSS variable system, ESLint/Prettier standards, and Husky-based pre-commit checks.

---

## Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Available Scripts](#available-scripts)
- [Development Server Configuration](#development-server-configuration)
- [Architecture](#architecture)
  - [Routing and Lazy Loading](#routing-and-lazy-loading)
  - [State Management](#state-management)
  - [Internationalization](#internationalization)
  - [Element Plus On-Demand Imports](#element-plus-on-demand-imports)
  - [SCSS Styling System](#scss-styling-system)
- [Configuration Files](#configuration-files)
- [Development Conventions](#development-conventions)
- [Formatting and ESLint Checks](#formatting-and-eslint-checks)
- [Login Page Notes](#login-page-notes)
- [DefaultLayout Notes](#defaultlayout-notes)
- [TransactionListView Notes](#transactionlistview-notes)

---

## Tech Stack

| Technology                                                | Version | Description                                                  |
| --------------------------------------------------------- | ------- | ------------------------------------------------------------ |
| [Vue](https://vuejs.org/)                                 | ^3.5    | Progressive JavaScript framework, Composition API            |
| [Vite](https://vite.dev/)                                 | ^6.x    | Next-generation ESM-based build tool                         |
| [TypeScript](https://www.typescriptlang.org/)             | ^5.x    | Static type system                                           |
| [Vue Router](https://router.vuejs.org/)                   | ^4.x    | Official router, all pages use lazy loading                  |
| [Pinia](https://pinia.vuejs.org/)                         | ^2.x    | Lightweight and type-safe state management                   |
| [Vue I18n](https://vue-i18n.intlify.dev/)                 | ^9.x    | Internationalization for Chinese and English                 |
| [Element Plus](https://element-plus.org/)                 | ^2.x    | Enterprise UI library with on-demand imports                 |
| [SCSS](https://sass-lang.com/)                            | ^1.x    | CSS preprocessor with globally injected variables and mixins |
| [ESLint](https://eslint.org/)                             | ^9.x    | Flat Config-based code quality checks                        |
| [Prettier](https://prettier.io/)                          | ^3.x    | Code formatting                                              |
| [Husky](https://typicode.github.io/husky/)                | ^9.x    | Git hooks management                                         |
| [lint-staged](https://github.com/lint-staged/lint-staged) | ^15.x   | Runs checks only for staged files                            |

---

## Project Structure

```text
console-fe/
├── build/                     # Build scripts
│   ├── copy-dist.js           # Recursively copies dist/ to static resources after build
│   ├── copyDesigner.js        # Copies saga-statemachine-designer to public/ before build
│   └── version-plugin.js      # Legacy Webpack plugin reference (superseded by vite.config.ts)
├── public/                    # Static assets not processed by the build
├── src/
│   ├── assets/                # Bundled static assets such as images and fonts
│   ├── components/            # Shared business components reused across pages
│   ├── i18n/                  # Internationalization
│   │   ├── index.ts           # i18n instance, initializes locale from localStorage
│   │   └── locales/
│   │       ├── zh-CN.ts       # Chinese locale messages
│   │       └── en-US.ts       # English locale messages
│   ├── layouts/               # Layout components
│   │   └── DefaultLayout.vue  # Default layout: sidebar + header + content area
│   ├── router/
│   │   └── index.ts           # Route definitions, all pages are lazily imported
│   ├── stores/                # Pinia stores in composition style
│   │   ├── app.ts             # Global app state, including locale persistence
│   │   └── counter.ts         # Demo counter store
│   ├── styles/                # Global SCSS styling system
│   │   ├── variables.scss     # Design tokens: colors, spacing, radius, fonts, shadows
│   │   ├── mixins.scss        # Shared mixins: flex, truncation, scrollbar, breakpoints
│   │   ├── reset.scss         # Plain CSS reset without SCSS variable dependencies
│   │   └── global.scss        # Global stylesheet entry: reset + base theme + utilities
│   ├── utils/                 # Utility functions
│   ├── api/                   # API request wrappers
│   ├── views/                 # Route page components
│   │   ├── HomeView.vue       # Home page: locale switch, counter, stack overview
│   │   ├── AboutView.vue      # About page: stack table
│   │   └── NotFoundView.vue   # 404 page
│   ├── App.vue                # Root component with el-config-provider locale sync
│   ├── main.ts                # App entry: mounts pinia, router, i18n, and global styles
│   ├── env.d.ts               # Vite client types and .vue module declarations
│   ├── auto-imports.d.ts      # Generated by unplugin-auto-import, do not edit manually
│   └── components.d.ts        # Generated by unplugin-vue-components, do not edit manually
├── .husky/
│   └── pre-commit             # Pre-commit hook running lint-staged
├── eslint.config.mjs          # ESLint Flat Config for Vue + TypeScript + Prettier
├── .prettierrc.json           # Prettier rules
├── .prettierignore            # Prettier ignore list
├── vite.config.ts             # Vite config: plugins, alias, SCSS global injection
├── tsconfig.json              # TS project reference entry
├── tsconfig.app.json          # TS config for src, including path aliases
├── tsconfig.node.json         # TS config for Node-side files such as vite.config.ts
└── package.json               # Dependencies, scripts, engines, and lint-staged config
```

---

## Quick Start

Requirement: Node.js >= 24

```bash
# Install dependencies
npm install

# Start the development server (default: http://localhost:5173)
npm run dev

# Build for production
npm run build

# Preview the production build
npm run preview
```

---

## Available Scripts

| Command              | Description                                                                                                        |
| -------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `npm run dev`        | Starts the Vite dev server with HMR                                                                                |
| `npm run build`      | Copies designer assets, runs TypeScript checks, builds the production bundle, then copies dist to static resources |
| `npm run preview`    | Previews the built dist output locally                                                                             |
| `npm run type-check` | Runs TypeScript type checking only                                                                                 |
| `npm run lint`       | Runs ESLint on .vue, .ts, and .tsx files                                                                           |
| `npm run lint:fix`   | Runs ESLint and auto-fixes fixable issues                                                                          |
| `npm run format`     | Formats .vue, .ts, .tsx, .scss, .css, and .json in src                                                             |

---

## Development Server Configuration

The Vite dev server uses port `5173` by default. It can be changed with `--port` or exposed to the network with `--host 0.0.0.0`.

Common variants:

```bash
# Expose to the local network
npm run dev -- --host 0.0.0.0

# Use a custom port
npm run dev -- --port 3000

# Use both a custom host and port
npm run dev -- --host 0.0.0.0 --port 3000
```

**API proxy example (optional)**:

To enable API proxying during local development, add the following to `vite.config.ts`:

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://127.0.0.1:8080',
      changeOrigin: true,
    },
  },
},
```

> **Note**: The API proxy is optional and has been removed from `vite.config.ts`. In production, reverse proxy handling is done by the backend gateway or Nginx.

---

## Architecture

### Routing and Lazy Loading

All route page components must use dynamic `import()` so that each page chunk loads only when first visited.

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
        component: () => import('@/views/HomeView.vue'),
      },
    ],
  },
];
```

Do not statically import page components in `router/index.ts`.

### State Management

The project uses Pinia in composition style with `defineStore` and `setup`, aligned with Vue 3 Composition API.

```ts
export const useAppStore = defineStore('app', () => {
  const locale = ref<'zh-CN' | 'en-US'>(localStorage.getItem('locale') ?? 'zh-CN');

  function setLocale(lang: 'zh-CN' | 'en-US') {
    locale.value = lang;
    i18n.global.locale.value = lang;
    localStorage.setItem('locale', lang);
  }

  return { locale, setLocale };
});
```

Add new business stores under `src/stores/`, split by domain such as `user.ts` or `transaction.ts`.

### Internationalization

Vue I18n 9 is used in composition mode with `legacy: false`. Locale state is managed centrally by the app store and persisted in `localStorage`.

Add new messages to both `src/i18n/locales/zh-CN.ts` and `src/i18n/locales/en-US.ts` with matching structures.

```ts
export default {
  common: { confirm: 'Confirm', cancel: 'Cancel' },
};
```

```vue
<script setup lang="ts">
const { t } = useI18n();
</script>

<template>
  <el-button>{{ t('common.confirm') }}</el-button>
</template>
```

To switch the locale programmatically:

```ts
const appStore = useAppStore();
appStore.setLocale('en-US'); // or 'zh-CN'
```

User-facing text must not be hardcoded in templates or logic.

### Element Plus On-Demand Imports

`unplugin-auto-import` and `unplugin-vue-components` are used so only the required Element Plus APIs and components are bundled.

```ts
AutoImport({
  resolvers: [ElementPlusResolver()],
  imports: ['vue', 'vue-router', 'pinia', 'vue-i18n'],
});
Components({ resolvers: [ElementPlusResolver()] });
```

Components and command-style APIs can be used directly without manual imports:

```vue
<template>
  <el-button type="primary">Button</el-button>
  <el-table :data="list" />
</template>

<script setup lang="ts">
// Command-style APIs are auto-imported as well
ElMessage.success('Done');
ElNotification({ title: 'Notice', message: 'Content' });
</script>
```

The generated `src/auto-imports.d.ts` and `src/components.d.ts` files must not be edited manually.

### SCSS Styling System

| File             | Responsibility | Description                                                  |
| ---------------- | -------------- | ------------------------------------------------------------ |
| `variables.scss` | Design tokens  | Colors, spacing, radius, fonts, shadows, layout variables    |
| `mixins.scss`    | Shared mixins  | Flex helpers, truncation, centering, scrollbars, breakpoints |
| `reset.scss`     | CSS reset      | Plain CSS reset without SCSS variable usage                  |
| `global.scss`    | Global entry   | Reset, base theme styles, and utility classes                |

Variables and mixins are injected globally from `vite.config.ts`, so SFC styles should use them directly without manual `@use`.

```vue
<style lang="scss" scoped>
.card {
  padding: $spacing-md;
  border-radius: $border-radius-md;
  @include flex-between;
  @include scrollbar(6px);
}
</style>
```

> **Note**: `additionalData` is only injected into SCSS files processed directly by Vite. Sub-modules imported with `@use` (such as `reset.scss`) are isolated Sass modules that cannot inherit the injected variables. This is why `reset.scss` contains only plain CSS without SCSS variable references.

---

## Configuration Files

### `vite.config.ts`

| Item                                          | Description                                                  |
| --------------------------------------------- | ------------------------------------------------------------ |
| `@vitejs/plugin-vue`                          | Compiles `.vue` single-file components                       |
| `unplugin-auto-import`                        | Auto-imports Vue, Router, Pinia, I18n, and Element Plus APIs |
| `unplugin-vue-components`                     | Auto-registers Element Plus components on demand             |
| `resolve.alias['@']`                          | Maps `@` to `src/`                                           |
| `css.preprocessorOptions.scss.additionalData` | Injects SCSS variables and mixins globally                   |

### `eslint.config.mjs`

- `eslint-plugin-vue` with `flat/recommended` for Vue 3 templates and `script setup`
- `@typescript-eslint` for TS-aware linting
- `eslint-config-prettier` to disable conflicting formatting rules

### TypeScript Project References

| File                 | Purpose                                  | Target |
| -------------------- | ---------------------------------------- | ------ |
| `tsconfig.json`      | Project reference entry                  | —      |
| `tsconfig.app.json`  | TypeScript config for `src/` and aliases | ES2020 |
| `tsconfig.node.json` | TypeScript config for Node-side files    | ES2022 |

---

## Development Conventions

### 1. Directory and Component Placement

Route entry components must live in `src/views/` and use the `*View.vue` naming pattern.

Private page sub-blocks should stay near the page, such as `src/views/home/components/`, instead of polluting global `components`.

Move a component to `src/components/` only when at least one of the following is true:

- It is reused by two or more pages or layouts.
- It has stable props and emits with clear business meaning.
- Extracting it significantly reduces duplicated template, style, or logic.

Layouts should only handle shell structure such as header, aside, main area, and footer. Business logic should stay in views or domain components.

### 2. Naming Rules

| Type             | Location               | Rule                                      | Example                             |
| ---------------- | ---------------------- | ----------------------------------------- | ----------------------------------- |
| Page component   | `src/views/`           | `*View.vue`, PascalCase                   | `TransactionListView.vue`           |
| Layout component | `src/layouts/`         | `*Layout.vue`, PascalCase                 | `DefaultLayout.vue`                 |
| Shared component | `src/components/`      | PascalCase, semantic prefixes preferred   | `AppHeader.vue`, `SeataTable.vue`   |
| Store            | `src/stores/`          | lowercase file name, export `useXxxStore` | `user.ts` → `useUserStore`          |
| Composable       | `src/utils/` or nearby | `useXxx.ts`                               | `usePagination.ts`                  |
| Event            | —                      | semantic verb phrases                     | `submit`, `confirm`, `changeLocale` |

### 3. Prefer Element Plus

Use Element Plus components by default for layout and interaction. Use native HTML only when there is no suitable Element Plus component.

### 4. Route Lazy Loading

```ts
component: () => import('@/views/MyView.vue');
```

Do not write:

```ts
import MyView from '@/views/MyView.vue';
component: MyView;
```

### 5. Internationalization Rules

All user-visible text should go through i18n. Every new key must be added to both locale files with aligned structure.

### 6. Style Rules

- Use `<style lang="scss" scoped>` by default.
- Use `:deep()` only when necessary.
- Do not manually `@use` `variables.scss` or `mixins.scss` inside SFC styles.
- Prefer BEM-style naming for new or refactored blocks.
- Use `is-*` and `has-*` for state classes.
- Avoid selectors nested deeper than three levels.
- Avoid meaningless class names such as `box1`, `left`, or `red-text`.
- Prefer SCSS variables over magic numbers.
- Keep reusable global utility classes in `src/styles/global.scss`.

### 7. Type Rules

- Prefer explicit typing and avoid `any`.
- Prefer generic `defineProps` and `defineEmits` usage.
- Do not manually edit generated files such as `src/auto-imports.d.ts` and `src/components.d.ts`.

```ts
defineProps<{ title: string; count?: number }>();
defineEmits<{ submit: [value: string]; close: [] }>();
```

### 8. API Layer

- Wrap API requests with `axios` under `src/api/`.
- Keep error handling and token management consistent.

### 9. Comment Language

All code comments must be written in English. Chinese comments are not allowed in source files such as `.vue`, `.ts`, `.js`, and `.scss`. Translation values in locale files are exempt.

### 10. TableActionColumn Rule

Every `el-table` with an actions column must use `src/components/TableActionColumn.vue`. Do not hand-write repeated button and dropdown templates inside tables.

```ts
import TableActionColumn, { type RowAction } from '@/components/TableActionColumn.vue';

const actions: RowAction[] = [
  { key: 'delete', labelKey: 'xxx.actions.delete', buttonType: 'danger' },
  { key: 'forceDelete', labelKey: 'xxx.actions.forceDelete' },
  { key: 'stopRetry', labelKey: 'xxx.actions.stopRetry' },
];
```

- `maxPrimary` defaults to `2` and controls how many actions stay visible before collapsing the rest into the More dropdown.
- The component calculates its own column width.
- Confirmation logic still belongs in the parent `handleAction` implementation.

### 11. Confirmation Dialog Rule

Every non-view action such as delete, force delete, stop retry, start retry, commit, rollback, or status update must be confirmed with `ElMessageBox.confirm` before sending the API request.

```ts
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
```

### 12. Layout Rules

For login and admin layouts, keep the full-height chain intact with `html`, `body`, and `#app`, and add `min-height: 0` to key flex containers.

For query-table pages:

- Use separate edit and submitted states, such as `formState` and `queryState`.
- Reset pagination to page 1 after search or reset.
- Keep the actions column fixed on the right.
- Show up to two common actions and collapse infrequent ones into More.
- Ensure responsive form degradation from multi-column desktop to single-column narrow screens.

---

## Formatting and ESLint Checks

### Manual Commands

```bash
npm run lint
npm run lint:fix
npm run format
npm run type-check
```

### Automatic Checks Before Commit

The `.husky/pre-commit` hook runs `lint-staged` for staged files only:

1. `eslint --fix`
2. `prettier --write`

Configured pattern in `package.json`:

```text
*.{vue,ts,tsx} → eslint --fix, prettier --write
```

If ESLint reports non-fixable errors, the commit is blocked until the issues are fixed and staged again.

### Prettier Rules

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

## Login Page Notes

The login page uses an isolated `PublicLayout`, fully separated from the post-login `DefaultLayout`.

### Layout Structure

```text
PublicLayout (src/layouts/PublicLayout.vue)
├── AppHeader
├── <router-view />
└── AppFooter
```

### Full-Height Strategy

Each layer in the full-height chain must be configured correctly, otherwise `height: 100%` will not work.

```text
html { height: 100% }
  └── body { height: 100% }
        └── #app { height: 100% }
              └── .public-layout { height: 100vh; display: flex; flex-direction: column }
                    └── .public-main { flex: 1; display: flex; flex-direction: column; min-height: 0 }
                          └── .login-page { flex: 1; display: flex; flex-direction: column }
                                └── .top-section { flex: 1; min-height: 600px }
```

Key points:

- Set `height: 100%` for `html`, `body`, and `#app` in `reset.scss`.
- When the parent is a flex column container, the child should use `flex: 1` instead of relying on `height: 100%`.
- Use `min-height: 0` to allow flex children to shrink correctly.

### Wide-Screen Constraint and Narrow-Screen Scroll

`.top-section` uses `overflow-x: auto`, and `.login-content` uses absolute positioning with `max-width` and centered margins.

### CSS Class Naming

| Class            | Description                                         |
| ---------------- | --------------------------------------------------- |
| `.login-page`    | Root login page container with flex column layout   |
| `.top-section`   | Main area with dark dotted background and particles |
| `.login-content` | Width-constrained centered content container        |
| `.product-area`  | Left product introduction section                   |
| `.login-panel`   | Right login form panel                              |
| `.login-card`    | Login card overriding default Element Plus padding  |
| `.login-warning` | Warning area with bottom spacing                    |

### i18n Namespaces

| Namespace     | Usage                  |
| ------------- | ---------------------- |
| `publicNav.*` | Header navigation text |
| `login.*`     | All login-page text    |

### Logo Assets

| File                              | Source                                            | Usage           |
| --------------------------------- | ------------------------------------------------- | --------------- |
| `src/assets/seata_logo.png`       | https://seata.apache.org/img/seata_logo.png       | Header logo     |
| `src/assets/seata_logo_white.png` | https://seata.apache.org/img/seata_logo_white.png | Login-side logo |

---

## DefaultLayout Notes

`DefaultLayout` is the admin layout after login, with a fixed header, a left sidebar, and a scrollable content area.

### Layout Structure

```text
DefaultLayout (src/layouts/DefaultLayout.vue)
├── AppHeader
├── .layout-body
│   ├── .layout-aside
│   │   └── AppSidebarMenu
│   └── .layout-main
│         └── <router-view />
└── AppFooter
```

### Full-Height Flex Strategy

```text
.layout-container { height: 100vh; display: flex; flex-direction: column; overflow: hidden }
  └── AppHeader
  └── .layout-body { flex: 1; min-height: 0; display: flex; overflow: hidden }
        └── .layout-aside { display: flex; flex-direction: column; min-height: 0 }
              └── AppSidebarMenu { flex: 1; min-height: 0 }
                    └── el-menu { height: 100% }
        └── .layout-main { flex: 1; overflow-y: auto }
  └── AppFooter
```

Key points:

- Set `min-height: 0` on `.layout-body`.
- Make `AppSidebarMenu` fill the remaining aside height with `flex: 1`.
- Let `.layout-main` scroll independently with `overflow-y: auto`.

### Single Source of Truth for Routes and Menu

Route definitions and sidebar menu items should be derived from the same route metadata so menus and routes are not maintained separately.

### AppSidebarMenu

When using `el-menu` with `:router="true"`, each `index` must exactly match the route path including the leading slash.

### CSS Class Naming

| Class               | Description                                     |
| ------------------- | ----------------------------------------------- |
| `.layout-container` | Root container with 100vh flex column layout    |
| `.layout-body`      | Middle area containing sidebar and content      |
| `.layout-aside`     | Fixed-width sidebar container                   |
| `.layout-main`      | Flexible and vertically scrollable content area |
| `.layout-footer`    | Footer with white background and top shadow     |

---

## TransactionListView Notes

`TransactionListView` is the reference implementation for a filter + table + pagination + row actions page.

### Lesson 1: Separate Edit State from Submitted Query State

- Use `formState` for live form input.
- Use `queryState` for the submitted query.
- Reset `currentPage = 1` after each search.

### Lesson 2: Prioritize Readability and Responsiveness in Filters

- Use CSS Grid such as `repeat(auto-fit, minmax(...))` for responsive filter layouts.
- Let date-range pickers span two columns on wide screens and degrade to one column on narrow screens.
- Keep the action button row on a full-width line.

### Lesson 3: Group Actions into Primary Actions and More

- Put common actions in visible buttons.
- Collapse infrequent actions into a dropdown.
- Keep the actions column fixed to the right.
- Use `show-overflow-tooltip` for long text fields.

### Lesson 4: Model Even Mock Data with Real Domain Semantics

- Define `TransactionRow` and `QueryState` explicitly.
- Keep pagination hooks ready for future backend integration.

### Hard Rules for Similar Pages

1. All page text must use i18n keys.
2. Query pages must separate edit state and submitted state.
3. Search and reset must both return pagination to page 1.
4. The actions column must stay fixed on the right and collapse infrequent actions into More.
5. Complex filter areas must support responsive degradation.
