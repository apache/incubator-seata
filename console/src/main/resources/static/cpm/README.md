<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->
# 连接池监控前端

这是一个用于监控数据库连接池的React前端应用，支持Druid和HikariCP两种连接池类型的监控。

## 功能特点

1. **筛选功能**
    - 连接池类型筛选：支持选择All/Druid/HikariCP三种连接池类型
    - 服务名称搜索：支持通过服务名称精确查询连接池信息

2. **核心指标展示**
    - 网格卡片布局展示各服务的核心连接池指标
    - 包括Active连接数、Idle连接数、Wait等待数、ExecuteCount执行数

3. **详细数据表格**
    - 服务列表表格，包含服务名称、连接池类型等信息
    - 支持展开查看详细配置信息

4. **数据可视化图表**
    - Druid连接池专属图表：
        * SQL执行折线图
        * 连接持有时间曲线图
        * 事务耗时分布柱状图
    - HikariCP连接池专属指标展示：
        * connectionAcquiredNanos
        * connectionTimeoutRate
        * leakDetectionThreshold

## 技术栈

- React 18
- TypeScript
- Ant Design 5.x
- Recharts (图表库)
- Axios (HTTP客户端)

## 数据源配置说明

本系统支持两种数据源模式：模拟数据和真实后端数据。

### 切换数据源

在 `src/services/api.ts` 文件中，通过修改 `USE_MOCK_DATA` 常量来切换数据源：

```javascript
// 判断是否使用模拟数据
const USE_MOCK_DATA = true;
```

- 当 `USE_MOCK_DATA = true` 时，系统使用模拟数据（来自 `mockData.ts`）
- 当 `USE_MOCK_DATA = false` 时，系统连接真实后端API

### 使用真实后端数据

要使用真实后端数据，需要确保：

1. 后端服务已启动并正常运行
2. 后端API接口路径正确
3. 将 `USE_MOCK_DATA` 设置为 `false`

## 项目结构

```
cpm/
├── public/                 # 静态资源
├── src/
│   ├── components/         # React组件
│   │   ├── charts/         # 图表相关组件
│   │   │   ├── DruidCharts.tsx
│   │   │   └── HikariCPMetricsPanel.tsx
│   │   ├── FilterBar.tsx   # 筛选组件
│   │   ├── MetricsGrid.tsx # 指标网格组件
│   │   └── PoolTable.tsx   # 连接池表格组件
│   ├── services/           # API服务
│   │   ├── api.ts          # API调用封装
│   │   └── mockData.ts     # 模拟数据生成
│   ├── types/              # TypeScript类型定义
│   │   └── index.ts
│   ├── App.tsx             # 应用主组件
│   ├── index.tsx           # 应用入口
│   └── index.css           # 全局样式
└── package.json            # 项目依赖配置
```

## 安装与运行

### 前提条件

- Node.js 14.x 或更高版本
- npm 6.x 或更高版本

### 安装依赖

```bash
cd cpm
npm install
```

### 开发模式运行

```bash
npm start
```

应用将在开发模式下运行，访问 [http://localhost:3000](http://localhost:3000) 查看。

### 生产环境构建

```bash
npm run build
```

构建后的文件将生成在 `build` 目录中。