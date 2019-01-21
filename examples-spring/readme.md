#基于spring-boot和spring-mvc的测试项目


##分布式事务相关类说明
 
**com.alibaba.feascar.example.inteceptor.TxRestTemplateInterceptor**: 

 RestTemplate请求拦截器,用于通过RestTemplate发起的RPC过程中事务Id的传递
 
**com.alibaba.feascar.example.interceptor.TxMvcInterceptor**: 

 spring-mvc请求拦截器,用于处理响应之前初始化事务Id
  
**com.alibaba.feascar.example.config.MvcConfig**: 

 spring-mvc配置类,增加请求拦截器TxMvcInterceptor 
 
**com.alibaba.feascar.example.config.RestConfig**: 

 RestTemplate配置类,增加请求拦截器TxRestTemplateInterceptor 
 
**com.alibaba.feascar.example.config.TransactionConfig**: 

 分布式事务配置类,实例化GlobalTransactionScanner类


##测试启用分布式事务的API

**step1. 启动fescar-server**

本项目中相关client配置为application.conf

**step2. 分别以如下profile启动Application**

 1. -Dspring.profiles.active=bussiness
 2. -Dspring.profiles.active=account
 3. -Dspring.profiles.active=order
 4. -Dspring.profiles.active=storage
 
**step3. 调用bussiness服务执行API验证**

* 正常执行,分布式事务应该提交: 

curl -X POST --header 'Content-Type: application/x-www-form-urlencoded' \
 -d 'userId=U100001&commodityCode=C00321&orderCount=10&rollback=false'  \
 http://localhost:8081/api/bussiness/purchase
 
* 异常执行,分布式事务应该回滚: 

curl -X POST --header 'Content-Type: application/x-www-form-urlencoded' \
 -d 'userId=U100001&commodityCode=C00321&orderCount=10&rollback=true'  \
 http://localhost:8081/api/bussiness/purchase

 
  





