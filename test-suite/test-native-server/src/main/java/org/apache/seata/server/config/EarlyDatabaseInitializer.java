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
package org.apache.seata.server.config;

import com.zaxxer.hikari.HikariDataSource;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Execute schema.sql and data.sql early, before any Spring beans are instantiated.
 * <p>
 * This is needed because Seata's {@code DataSourceProxy} checks for the
 * {@code undo_log} table during {@code BeanPostProcessor} processing — before
 * Spring Boot's {@code DataSourceScriptDatabaseInitializer} (which normally
 * runs schema.sql) gets a chance to execute. By running the DDL/DML scripts
 * in a {@link BeanFactoryPostProcessor}, we guarantee the tables exist before
 * the DataSource bean is created and wrapped by Seata.
 */
@Configuration
public class EarlyDatabaseInitializer implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(EarlyDatabaseInitializer.class);

    private Environment environment;

    @NullMarked
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @NullMarked
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");

        if (url == null) {
            LOGGER.warn("spring.datasource.url is not set, skipping early database initialization");
            return;
        }

        LOGGER.info("Initializing database schema and data early (before Seata DataSource proxy)...");

        try (HikariDataSource tempDataSource = new HikariDataSource()) {
            tempDataSource.setJdbcUrl(url);
            if (username != null) {
                tempDataSource.setUsername(username);
            }
            if (password != null) {
                tempDataSource.setPassword(password);
            }
            if (driverClassName != null) {
                tempDataSource.setDriverClassName(driverClassName);
            }
            tempDataSource.setMaximumPoolSize(2);
            tempDataSource.setPoolName("EarlyDbInit");

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema.sql"));
            populator.addScript(new ClassPathResource("data.sql"));
            populator.setContinueOnError(false);
            DatabasePopulatorUtils.execute(populator, tempDataSource);
            LOGGER.info("Early database initialization completed successfully.");
        }
    }
}
