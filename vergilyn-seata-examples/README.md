# vergilyn-seata-examples

以 [seata.io][seata.io zh-cn docs] 官方文档中提到的示例作为测试代码。  
通过测试代码 debug 调试和阅读 seata源码的实现原理。

## dependencies

nacos: [1.2.0-beta.0](https://github.com/alibaba/nacos/releases/tag/1.2.0-beta.0)
redis: win10, v3.2.100
mysql: mysql-5.7.25-winx64

| project  | server.port  | management.server.port |
| :-----   | :----------: |:----------------------:|
| NACOS    | 8848         | -----                  |
| GATEWAY  | 9000         | 19000                  |
| BUSINESS | 901X         | 1901X                  |
| ACCOUNT  | 902X         | 1902X                  |
| ORDER    | 903X         | 1903X                  |
| STORAGE  | 904X         | 1904X                  |


[seata.io zh-cn docs]: https://seata.io/zh-cn/docs/overview/what-is-seata.html
