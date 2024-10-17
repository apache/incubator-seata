FROM java:8
# 服务器只有dockerfile和jar在同级目录
#VOLUME /home/xjl/code/seata-e2e/e2e-data/nacos-springcloud-alibaba/service-consumer/jar:/jar
ADD ROOT/*.jar /app.jar
CMD ["--server.port=8080"]
# 指定容器内要暴露的端口
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]