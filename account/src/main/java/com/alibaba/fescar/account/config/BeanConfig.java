package com.alibaba.fescar.account.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

//@Configuration
public class BeanConfig {
    @Bean
    GlobalTransactionScanner scanner() {
        return new GlobalTransactionScanner("dubbo-demo-order-service", "my_test_tx_group");
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl("jdbc:mysql://47.100.229.3:3306/account?useUnicode=true&amp;characterEncoding=utf8&amp;autoReconnect=true&amp;");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("WO86yanya");
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        return new DataSourceProxy(druidDataSource);
    }
}
