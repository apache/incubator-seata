[English](README.md) | 简体中文

# Seata Saga StateMachine Designer

Seata Saga 状态机可视化图形设计器, 基于 [GGEditor](https://github.com/alibaba/GGEditor)

## 运行

```sh
$ git clone https://github.com/seata/seata.git
$ cd saga/saga-statemachine-designer
$ npm install
$ npm start
```

## 打包
```sh
$ cd saga/saga-statemachine-designer
$ npm build
```

然后将index.html和dist目录拷贝到web server的静态页面目录下

## 使用
了解状态机的种状态类型，请看Saga的[文档](http://seata.io/zh-cn/docs/user/saga.html)。 通过设计器完成设计后可以点击工具栏的'Json View'按钮切换到Json视图，将Json拷贝保存到自己应用的工程里。虽然设计器生成的Json与Saga标准的Json有所差别（因为设计器生成的json带有布局信息），但状态机可以直接加载，它会将其转化成Saga状态机标准的Json。