package com.alibaba.fescar.example.config;

import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TransactionConfig {

    @Autowired
    Environment env;

    @Bean
    public GlobalTransactionScanner globalTransactionScanner() {
        String applicationId = env.getProperty("spring.application.name");
        String txServiceGroup = env.getProperty("app.fescar.txServiceGroup");
        GlobalTransactionScanner scanner = new GlobalTransactionScanner(applicationId, txServiceGroup);
        return scanner;
    }
}
