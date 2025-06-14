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
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

/**
 * Tests for {@link SeataDataSourceAutoConfiguration} to verify conditional bean registration.
 */
public class SeataDataSourceAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SeataDataSourceAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(SeataProperties.class, SeataProperties::new)
            .withBean(SpringCloudAlibabaConfiguration.class, SpringCloudAlibabaConfiguration::new);

    @Test
    void whenConditionsMet_thenAutoDataSourceProxyCreatorCreated() {
        try (MockedConstruction<DataSourceProxy> mocked = mockConstruction(DataSourceProxy.class)) {
            contextRunner
                    .withPropertyValues(
                            "seata.enabled=true",
                            "seata.enableAutoDataSourceProxy=true",
                            "seata.enable-auto-data-source-proxy=true"
                    )
                    .run(context -> {
                        // assert DataSourceProxy construction to be mock
                        assertThat(mocked.constructed()).isNotEmpty();
                        assertThat(context).hasSingleBean(SeataAutoDataSourceProxyCreator.class);
                    });
        }
    }

    @Test
    void whenDisabledByProperty_thenBeanNotCreated() {
        contextRunner
                .withPropertyValues(
                        "seata.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SeataAutoDataSourceProxyCreator.class);
                });
    }

    @Test
    void whenNoDataSourceBean_thenBeanNotCreated() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SeataDataSourceAutoConfiguration.class))
                .withBean(SeataProperties.class, SeataProperties::new)
                .withBean(SpringCloudAlibabaConfiguration.class, SpringCloudAlibabaConfiguration::new)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SeataAutoDataSourceProxyCreator.class);
                });
    }
}
