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
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.core.model.Resource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.StateMachineEngine;
import org.apache.seata.saga.engine.config.DbStateMachineConfig;
import org.apache.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import org.apache.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.apache.seata.tm.TMClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.seata.spring.boot.autoconfigure.SeataSagaAutoConfiguration.SAGA_ASYNC_THREAD_POOL_EXECUTOR_BEAN_NAME;
import static org.apache.seata.spring.boot.autoconfigure.SeataSagaAutoConfiguration.SAGA_REJECTED_EXECUTION_HANDLER_BEAN_NAME;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SeataSagaAutoConfiguration} to verify conditional bean registration.
 */
@SpringBootTest(
        classes = {SeataSagaAutoConfigurationTest.TestConfig.class},
        properties = {
            "seata.enabled=true",
            "seata.saga.enabled=true",
            "spring.application.name=testApp",
            "seata.tx-service-group=testTxGroup",
            "seata.saga.state-machine.enable-async=true"
        })
class SeataSagaAutoConfigurationTest {
    @Autowired
    private ApplicationContext applicationContext;

    private static MockedStatic<TMClient> mockedTMClient;

    private static MockedStatic<DefaultResourceManager> mockedDefaultResourceManager;

    @Autowired(required = false)
    @Qualifier(SAGA_ASYNC_THREAD_POOL_EXECUTOR_BEAN_NAME)
    private ThreadPoolExecutor asyncExecutor;

    @Autowired(required = false)
    @Qualifier(SAGA_REJECTED_EXECUTION_HANDLER_BEAN_NAME)
    private RejectedExecutionHandler rejectedExecutionHandler;

    @BeforeAll
    static void mockStaticInit() {
        mockedTMClient = Mockito.mockStatic(TMClient.class);
        mockedTMClient.when(() -> TMClient.init(any(), any(), any(), any())).then(invocation -> null);
        mockedDefaultResourceManager = Mockito.mockStatic(DefaultResourceManager.class);
        DefaultResourceManager mockResourceManager = mock(DefaultResourceManager.class);
        doAnswer(invocation -> null).when(mockResourceManager).registerResource(any(Resource.class));
        mockedDefaultResourceManager.when(DefaultResourceManager::get).thenReturn(mockResourceManager);
    }

    @AfterAll
    static void closeMock() {
        mockedTMClient.close();
        mockedDefaultResourceManager.close();
    }

    @Configuration
    @EnableConfigurationProperties(SeataProperties.class)
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        SeataCoreAutoConfiguration.class,
        SeataAutoConfiguration.class,
        SeataSagaAutoConfiguration.class
    })
    static class TestConfig {
        @Bean
        public DataSource dataSource() throws SQLException {
            Connection mockConnection = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            when(mockConnection.getMetaData()).thenReturn(metaData);
            DataSource mockDataSource = mock(DataSource.class);
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            return mockDataSource;
        }
    }

    @Test
    void testDbStateMachineConfigAndEngineBeanCreation() {
        StateMachineConfig stateMachineConfig = applicationContext.getBean(StateMachineConfig.class);
        AssertionsForClassTypes.assertThat(stateMachineConfig).isNotNull();
        assertThat(stateMachineConfig).isInstanceOf(DbStateMachineConfig.class);

        StateMachineEngine stateMachineEngine = applicationContext.getBean(StateMachineEngine.class);
        AssertionsForClassTypes.assertThat(stateMachineEngine).isNotNull();
        assertThat(stateMachineEngine).isInstanceOf(ProcessCtrlStateMachineEngine.class);
    }

    @Test
    void testAsyncThreadPoolExecutorCreation() {
        assertThat(asyncExecutor).isNotNull();
        assertThat(rejectedExecutionHandler).isNotNull();
        assertThat(rejectedExecutionHandler).isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }
}
