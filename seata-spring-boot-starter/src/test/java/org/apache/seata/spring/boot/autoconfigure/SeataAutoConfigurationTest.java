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

import org.apache.seata.spring.annotation.GlobalTransactionScanner;
import org.apache.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.apache.seata.tm.TMClient;
import org.apache.seata.tm.api.DefaultFailureHandlerImpl;
import org.apache.seata.tm.api.FailureHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link SeataAutoConfiguration} to verify conditional bean registration.
 * Here not use new ApplicationContextRunner() to avoid environment to be null.
 */
@SpringBootTest(
        classes = SeataAutoConfigurationTest.TestConfig.class,
        properties = {
                "seata.enabled=true",
                "seata.application-id=testApp",
                "seata.tx-service-group=test_tx_group"
        }
)
public class SeataAutoConfigurationTest {

    private static MockedStatic<TMClient> mockedTMClient;

    @BeforeAll
    static void mockStaticInit() {
        mockedTMClient = Mockito.mockStatic(TMClient.class);
        mockedTMClient.when(() -> TMClient.init(any(), any(), any(), any())).then(invocation -> null);
    }

    @AfterAll
    static void closeMock() {
        mockedTMClient.close();
    }

    @Configuration
    @EnableConfigurationProperties(SeataProperties.class)
    @ImportAutoConfiguration({SeataCoreAutoConfiguration.class, SeataAutoConfiguration.class})
    static class TestConfig {
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testFailureHandlerBeanCreated() {
        assertThat(applicationContext.containsBean("failureHandler")).isTrue();
        FailureHandler failureHandler = applicationContext.getBean(FailureHandler.class);
        assertThat(failureHandler).isNotNull();
        assertThat(failureHandler).isInstanceOf(DefaultFailureHandlerImpl.class);
    }

    @Test
    void testGlobalTransactionScannerBeanCreated() {
        assertThat(applicationContext.containsBean("globalTransactionScanner")).isTrue();
        GlobalTransactionScanner scanner = applicationContext.getBean(GlobalTransactionScanner.class);
        assertThat(scanner).isNotNull();
    }
}
