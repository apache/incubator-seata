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
import org.apache.seata.spring.boot.autoconfigure.properties.SpringCloudAlibabaConfiguration;
import org.apache.seata.tm.TMClient;
import org.apache.seata.tm.api.DefaultFailureHandlerImpl;
import org.apache.seata.tm.api.FailureHandler;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link SeataAutoConfiguration} to verify conditional bean registration.
 * Here not use new ApplicationContextRunner() to avoid environment to be null.
 */
@SpringBootTest(
        classes = SeataAutoConfigurationTest.TestConfig.class,
        properties = {"seata.enabled=true", "seata.application-id=testApp", "seata.tx-service-group=test_tx_group"})
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
    static class TestConfig {}

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

    @Test
    void testGlobalTransactionScannerProperties() {
        GlobalTransactionScanner scanner = applicationContext.getBean(GlobalTransactionScanner.class);
        assertThat(scanner).isNotNull();
        // Verify scanner is configured with test properties
        assertThat(scanner.getApplicationId()).isEqualTo("testApp");
        assertThat(scanner.getTxServiceGroup()).isEqualTo("test_tx_group");
    }

    @Test
    void testSeataPropertiesLoaded() {
        // SeataProperties may be registered twice (@Component + @EnableConfigurationProperties),
        // so we use getBeansOfType to get any instance
        Map<String, SeataProperties> beans = applicationContext.getBeansOfType(SeataProperties.class);
        assertThat(beans.isEmpty()).isFalse();
        SeataProperties seataProperties = beans.values().iterator().next();
        assertThat(seataProperties).isNotNull();
        assertThat(seataProperties.isEnabled()).isTrue();
        assertThat(seataProperties.getApplicationId()).isEqualTo("testApp");
        assertThat(seataProperties.getTxServiceGroup()).isEqualTo("test_tx_group");
    }

    /**
     * Use ApplicationContextRunner for lightweight testing of disabled scenarios
     * and various property configurations, avoiding nested @SpringBootTest issues.
     */
    @Test
    void testSeataDisabled() {
        // Use a lightweight ApplicationContextRunner to test the disabled scenario
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(SeataCoreAutoConfiguration.class, SeataAutoConfiguration.class))
                .withBean(SeataProperties.class, SeataProperties::new)
                .withBean(SpringCloudAlibabaConfiguration.class, SpringCloudAlibabaConfiguration::new);

        contextRunner.withPropertyValues("seata.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(FailureHandler.class);
            assertThat(context).doesNotHaveBean(GlobalTransactionScanner.class);
        });
    }

    @Test
    void testDefaultSeataPropertiesValues() {
        SeataProperties props = new SeataProperties();
        // defaults
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.isEnableAutoDataSourceProxy()).isTrue();
        assertThat(props.isUseJdkProxy()).isFalse();
        assertThat(props.isExposeProxy()).isFalse();
        assertThat(props.getScanPackages()).isEmpty();
        assertThat(props.getExcludesForScanning()).isEmpty();
        assertThat(props.getExcludesForAutoProxying()).isEmpty();
    }

    @Test
    void testSeataPropertiesSetters() {
        SeataProperties props = new SeataProperties();
        props.setEnabled(false);
        assertThat(props.isEnabled()).isFalse();

        props.setApplicationId("myApp");
        assertThat(props.getApplicationId()).isEqualTo("myApp");

        props.setTxServiceGroup("myGroup");
        assertThat(props.getTxServiceGroup()).isEqualTo("myGroup");

        props.setUseJdkProxy(true);
        assertThat(props.isUseJdkProxy()).isTrue();

        props.setExposeProxy(true);
        assertThat(props.isExposeProxy()).isTrue();

        props.setDataSourceProxyMode("XA");
        assertThat(props.getDataSourceProxyMode()).isEqualTo("XA");

        props.setScanPackages(new String[] {"com.example"});
        assertThat(props.getScanPackages()).hasSize(1);

        props.setExcludesForScanning(new String[] {"exclude1", "exclude2"});
        assertThat(props.getExcludesForScanning()).hasSize(2);

        props.setExcludesForAutoProxying(new String[] {"ds1"});
        assertThat(props.getExcludesForAutoProxying()).hasSize(1);

        props.setAccessKey("ak");
        assertThat(props.getAccessKey()).isEqualTo("ak");

        props.setSecretKey("sk");
        assertThat(props.getSecretKey()).isEqualTo("sk");

        props.setEnableAutoDataSourceProxy(false);
        assertThat(props.isEnableAutoDataSourceProxy()).isFalse();
    }
}
