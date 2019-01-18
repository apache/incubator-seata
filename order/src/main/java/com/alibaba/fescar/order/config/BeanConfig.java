package com.alibaba.fescar.order.config;

import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    GlobalTransactionScanner scanner() {
        return new GlobalTransactionScanner("dubbo-demo-app", "my_test_tx_group");
    }

}
