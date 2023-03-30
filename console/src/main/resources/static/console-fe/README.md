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
