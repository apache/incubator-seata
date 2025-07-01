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
package org.apache.seata.spring.rm.fence;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.apache.seata.rm.fence.SpringFenceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SpringFenceConfigTest {

    private DataSource dataSource;
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource();
        transactionManager = mock(PlatformTransactionManager.class);
    }

    @Test
    void testAfterPropertiesSetThenSuccess() {
        SpringFenceConfig config = new SpringFenceConfig(dataSource, transactionManager);
        config.afterPropertiesSet();
    }

    @Test
    void testAfterPropertiesSetNullDataSourceShouldThrow() {
        SpringFenceConfig config = new SpringFenceConfig(null, transactionManager);

        assertThatThrownBy(config::afterPropertiesSet)
                .isInstanceOf(CommonFenceException.class)
                .hasMessageContaining(FrameworkErrorCode.DateSourceNeedInjected.getErrMessage());
    }

    @Test
    void testAfterPropertiesSetNullTransactionManagerShouldThrow() {
        SpringFenceConfig config = new SpringFenceConfig(dataSource, null);

        assertThatThrownBy(config::afterPropertiesSet)
                .isInstanceOf(CommonFenceException.class)
                .hasMessageContaining(FrameworkErrorCode.TransactionManagerNeedInjected.getErrMessage());
    }
}

