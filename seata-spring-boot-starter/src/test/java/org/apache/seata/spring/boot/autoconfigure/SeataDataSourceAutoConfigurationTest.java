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

import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.spring.annotation.datasource.SeataAutoDataSourceProxyCreator;
import org.apache.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.SpringCloudAlibabaConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

/**
 * Tests for {@link SeataDataSourceAutoConfiguration} to verify conditional bean registration.
 */
public class SeataDataSourceAutoConfigurationTest {

    @Configuration
    @EnableConfigurationProperties({SeataProperties.class, SpringCloudAlibabaConfiguration.class})
    static class PropertiesConfig {}

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SeataDataSourceAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class)
            .withBean(DataSource.class, () -> mock(DataSource.class));

    @Test
    void whenConditionsMet_thenAutoDataSourceProxyCreatorCreated() {
        try (MockedConstruction<DataSourceProxy> mocked = mockConstruction(DataSourceProxy.class)) {
            contextRunner
                    .withPropertyValues(
                            "seata.enabled=true",
                            "seata.enableAutoDataSourceProxy=true",
                            "seata.enable-auto-data-source-proxy=true")
                    .run(context -> {
                        // assert DataSourceProxy construction to be mock
                        assertThat(mocked.constructed()).isNotEmpty();
                        assertThat(context).hasSingleBean(SeataAutoDataSourceProxyCreator.class);
                    });
        }
    }

    @Test
    void whenDisabledByProperty_thenBeanNotCreated() {
        contextRunner.withPropertyValues("seata.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(SeataAutoDataSourceProxyCreator.class);
        });
    }

    @Test
    void whenNoDataSourceBean_thenBeanNotCreated() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SeataDataSourceAutoConfiguration.class))
                .withUserConfiguration(PropertiesConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SeataAutoDataSourceProxyCreator.class);
                });
    }

    @Test
    void whenEnableAutoDataSourceProxyFalse_thenBeanNotCreated() {
        contextRunner
                .withPropertyValues("seata.enabled=true", "seata.enableAutoDataSourceProxy=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SeataAutoDataSourceProxyCreator.class);
                });
    }

    @Test
    void whenEnableAutoDataSourceProxyKebabCaseFalse_thenBeanNotCreated() {
        contextRunner
                .withPropertyValues("seata.enabled=true", "seata.enable-auto-data-source-proxy=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SeataAutoDataSourceProxyCreator.class);
                });
    }

    @Test
    void whenUseJdkProxyTrue_thenProxyCreatorConfigured() {
        try (MockedConstruction<DataSourceProxy> mocked = mockConstruction(DataSourceProxy.class)) {
            contextRunner
                    .withPropertyValues(
                            "seata.enabled=true",
                            "seata.enableAutoDataSourceProxy=true",
                            "seata.enable-auto-data-source-proxy=true",
                            "seata.use-jdk-proxy=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(SeataAutoDataSourceProxyCreator.class);
                        SeataProperties properties = context.getBean(SeataProperties.class);
                        assertThat(properties.isUseJdkProxy()).isTrue();
                    });
        }
    }

    @Test
    void whenUseJdkProxyFalse_thenCglibProxyUsed() {
        try (MockedConstruction<DataSourceProxy> mocked = mockConstruction(DataSourceProxy.class)) {
            contextRunner
                    .withPropertyValues(
                            "seata.enabled=true",
                            "seata.enableAutoDataSourceProxy=true",
                            "seata.enable-auto-data-source-proxy=true",
                            "seata.use-jdk-proxy=false")
                    .run(context -> {
                        assertThat(context).hasSingleBean(SeataAutoDataSourceProxyCreator.class);
                        SeataProperties properties = context.getBean(SeataProperties.class);
                        assertThat(properties.isUseJdkProxy()).isFalse();
                    });
        }
    }

    @Test
    void whenExcludesForAutoProxyingSet_thenConfigured() {
        try (MockedConstruction<DataSourceProxy> mocked = mockConstruction(DataSourceProxy.class)) {
            contextRunner
                    .withPropertyValues(
                            "seata.enabled=true",
                            "seata.enableAutoDataSourceProxy=true",
                            "seata.enable-auto-data-source-proxy=true",
                            "seata.excludes-for-auto-proxying[0]=testDataSource",
                            "seata.excludes-for-auto-proxying[1]=anotherDataSource")
                    .run(context -> {
                        assertThat(context).hasSingleBean(SeataAutoDataSourceProxyCreator.class);
                        SeataProperties properties = context.getBean(SeataProperties.class);
                        assertThat(properties.getExcludesForAutoProxying()).hasSize(2);
                        assertThat(properties.getExcludesForAutoProxying())
                                .contains("testDataSource", "anotherDataSource");
                    });
        }
    }

    @Test
    void whenDataSourceProxyModeSet_thenConfigured() {
        try (MockedConstruction<DataSourceProxy> mocked = mockConstruction(DataSourceProxy.class)) {
            contextRunner
                    .withPropertyValues(
                            "seata.enabled=true",
                            "seata.enableAutoDataSourceProxy=true",
                            "seata.enable-auto-data-source-proxy=true",
                            "seata.data-source-proxy-mode=AT")
                    .run(context -> {
                        assertThat(context).hasSingleBean(SeataAutoDataSourceProxyCreator.class);
                        SeataProperties properties = context.getBean(SeataProperties.class);
                        assertThat(properties.getDataSourceProxyMode()).isEqualTo("AT");
                    });
        }
    }
}
