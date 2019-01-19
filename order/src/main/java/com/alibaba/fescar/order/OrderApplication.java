package com.alibaba.fescar.order;

import com.alibaba.fescar.rm.RMClientAT;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        RMClientAT.init("dubbo-demo-app", "my_test_tx_group");
        SpringApplication.run(OrderApplication.class, args);
    }

}

