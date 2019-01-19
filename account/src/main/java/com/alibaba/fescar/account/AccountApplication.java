package com.alibaba.fescar.account;

import com.alibaba.fescar.rm.RMClientAT;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AccountApplication {

    public static void main(String[] args) {
        RMClientAT.init("dubbo-demo-order-service", "my_test_tx_group");
        SpringApplication.run(AccountApplication.class, args);
    }

}

