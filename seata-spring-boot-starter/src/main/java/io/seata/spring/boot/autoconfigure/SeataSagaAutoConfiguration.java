/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure;

import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.config.DbStateMachineConfig;
import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import io.seata.saga.rm.StateMachineEngineHolder;
import io.seata.spring.boot.autoconfigure.properties.SeataSagaThreadPoolProperties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

/**
 * Saga auto configuration.
 *
 * @author wang.liang
 * @date 2020/4/15
 */
@Configuration
@ConditionalOnProperty(StarterConstants.SEATA_PREFIX + ".saga.enabled")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties({SeataSagaThreadPoolProperties.class})
public class SeataSagaAutoConfiguration {

    /**
     * Create state machine thread pool bean.
     */
    @Bean
    public ThreadPoolExecutor sagaStateMachineThreadPoolExecutor(SeataSagaThreadPoolProperties properties) {
        ThreadPoolExecutorFactoryBean threadFactory = new ThreadPoolExecutorFactoryBean();
        threadFactory.setCorePoolSize(properties.getCorePoolSize());
        threadFactory.setMaxPoolSize(properties.getMaxPoolSize());
        threadFactory.setKeepAliveSeconds(properties.getKeepAliveTime());

        BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            properties.getCorePoolSize(),
            properties.getMaxPoolSize(),
            properties.getKeepAliveTime(),
            TimeUnit.SECONDS,
            queue,
            threadFactory
        );

        return threadPoolExecutor;
    }

    /**
     * Create state machine config bean.
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    @ConfigurationProperties(StarterConstants.SEATA_PREFIX + ".saga.state-machine")
    public DbStateMachineConfig dbStateMachineConfig(DataSource dataSource, ThreadPoolExecutor threadPoolExecutor) {
        DbStateMachineConfig config = new DbStateMachineConfig();
        config.setDataSource(dataSource);
        config.setThreadPoolExecutor(threadPoolExecutor);
        return config;
    }

    /**
     * Create state machine engine bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public StateMachineEngine stateMachineEngine(StateMachineConfig config) {
        ProcessCtrlStateMachineEngine engine = new ProcessCtrlStateMachineEngine();
        engine.setStateMachineConfig(config);
        new StateMachineEngineHolder().setStateMachineEngine(engine);
        return engine;
    }
}
