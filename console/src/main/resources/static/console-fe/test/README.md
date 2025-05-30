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

## 使用说明

### 安装依赖
```sh
npm install uirecorder mocha -g
npm install
```

### 安装chrome浏览器插件
```sh
npm run installdriver
```

### 开始录制测试用例
```sh
// xxx.spec.js 为你的测试用例文件名称
uirecorder sample/xxx.spec.js
```

### 回归测试
#### 启动服务
```sh
npm run server
```

#### 单个文件测试
```sh
// xxx.spec.js 为你的测试用例文件名称
npm run singletest sample/xxx.spec.js
```

#### 并发测试
```sh
npm run paralleltest
```

### 查看报告
```sh
open reports/index.html
```
