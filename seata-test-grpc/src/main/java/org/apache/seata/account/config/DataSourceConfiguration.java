/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.account.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DataSource Configuration
 */
@Configuration
@PropertySource("classpath:application.properties")
public class DataSourceConfiguration {


    @Value("${spring.datasource.account.driverClassName}")
    private String driverClassName;
    @Value("${spring.datasource.account.url}")
    private String url;
    @Value("${spring.datasource.account.username}")
    private String userName;
    @Value("${spring.datasource.account.password}")
    private String password;

    @Bean
    public DataSource accountDataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUsername(userName);
        druidDataSource.setPassword(password);
        druidDataSource.setUrl(url);
        druidDataSource.setDriverClassName(driverClassName);
        return druidDataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(@Qualifier("accountDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
