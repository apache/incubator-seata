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

# 开始项目
## cnpm 安装（可忽略）
```sh
npm install -g cnpm --registry=https://registry.npmmirror.com

# 设置匿名
alias cnpm="npm --registry=https://registry.npmmirror.com \
--cache=$HOME/.npm/.cache/cnpm \
--disturl=https://npmmirror.com/mirrors/node \
--userconfig=$HOME/.cnpmrc"

# Or alias it in .bashrc or .zshrc
$ echo '\n#alias for cnpm\nalias cnpm="npm --registry=https://registry.npmmirror.com \
  --cache=$HOME/.npm/.cache/cnpm \
  --disturl=https://npmmirror.com/mirrors/node \
  --userconfig=$HOME/.cnpmrc"' >> ~/.zshrc && source ~/.zshrc

```
[详情地址: https://npmmirror.com/](https://npmmirror.com/) 

## 安装依赖
```sh
yarn
```
或
```
cnpm install
```

## 启动
```sh
yarn start
```
或
```
npm start
```

## 构建打包
```sh
yarn build
```
或
```
npm run build
```
## 

# 代理配置
`build/webpack.dev.conf.js`
修改proxy属性

```
proxy: [{
  context: ['/'],
  changeOrigin: true,
  secure: false,
  target: 'http://ip:port',
}],
```
