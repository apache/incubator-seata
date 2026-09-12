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
package org.apache.seata.rm.fence;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for SpringFenceConfig
 * Uses MockedStatic to avoid static state pollution
 * This version is completely isolated and safe from global test pollution
 */
@ExtendWith(MockitoExtension.class)
public class SpringFenceConfigTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private PlatformTransactionManager transactionManager;

    private SpringFenceConfig springFenceConfig;

    @BeforeEach
    public void setUp() {
        springFenceConfig = new SpringFenceConfig(dataSource, transactionManager);
    }

    @Test
    public void testConstructorWithValidParameters() {
        // Given
        DataSource testDataSource = dataSource;
        PlatformTransactionManager testTransactionManager = transactionManager;

        // When
        SpringFenceConfig config = new SpringFenceConfig(testDataSource, testTransactionManager);

        // Then
        assertNotNull(config);
    }

    @Test
    public void testAfterPropertiesSetWithValidDataSourceAndTransactionManager() {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            // When
            springFenceConfig.afterPropertiesSet();

            // Then
            mockedStatic.verify(() -> SpringFenceHandler.setDataSource(dataSource));
            mockedStatic.verify(() -> SpringFenceHandler.setTransactionTemplate(any(TransactionTemplate.class)));
        }
    }

    @Test
    public void testAfterPropertiesSetWithNullDataSource() {
        // Given
        SpringFenceConfig configWithNullDataSource = new SpringFenceConfig(null, transactionManager);

        // When & Then
        CommonFenceException exception = assertThrows(CommonFenceException.class, () -> {
            configWithNullDataSource.afterPropertiesSet();
        });

        assertEquals(FrameworkErrorCode.DateSourceNeedInjected, exception.getErrcode());
    }

    @Test
    public void testAfterPropertiesSetWithNullTransactionManager() {
        // Given
        SpringFenceConfig configWithNullTxManager = new SpringFenceConfig(dataSource, null);

        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            // When & Then
            CommonFenceException exception = assertThrows(CommonFenceException.class, () -> {
                configWithNullTxManager.afterPropertiesSet();
            });

            assertEquals(FrameworkErrorCode.TransactionManagerNeedInjected, exception.getErrcode());
        }
    }

    @Test
    public void testAfterPropertiesSetWithBothNullParameters() {
        // Given
        SpringFenceConfig configWithBothNull = new SpringFenceConfig(null, null);

        // When & Then
        CommonFenceException exception = assertThrows(CommonFenceException.class, () -> {
            configWithBothNull.afterPropertiesSet();
        });

        assertEquals(FrameworkErrorCode.DateSourceNeedInjected, exception.getErrcode());
    }
}
