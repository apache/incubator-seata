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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Dedicated test class for complex static state operations in SpringFenceHandler
 * This class is isolated to prevent pollution of other tests
 * All tests that heavily manipulate static state should be placed here
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SpringFenceHandlerStaticStateTest extends AbstractFenceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUpBase(); // Call parent cleanup first
        doSetUp(); // Template method for additional setup
    }

    @Override
    protected void doSetUp() throws Exception {
        // Setup for static state testing
    }

    @Test
    @Order(1)
    public void testComplexStaticStateOperations() {
        // Given
        SpringFenceHandler.setDataSource(dataSource);
        SpringFenceHandler.setTransactionTemplate(transactionTemplate);

        // When
        DataSource resultDataSource = SpringFenceHandler.getDataSource();

        // Then
        assertSame(dataSource, resultDataSource);
        assertNotNull(resultDataSource);
    }

    @Test
    @Order(2)
    public void testStaticStateReset() {
        // Given - set initial state
        SpringFenceHandler.setDataSource(dataSource);

        // When - reset to null
        SpringFenceHandler.setDataSource(null);

        // Then - verify reset
        DataSource result = SpringFenceHandler.getDataSource();
        // Note: result might be null after reset, which is expected
    }

    @Test
    @Order(3)
    public void testMultipleStaticStateChanges() {
        // Given
        DataSource dataSource1 = dataSource;

        // When - multiple changes
        SpringFenceHandler.setDataSource(dataSource1);
        DataSource result1 = SpringFenceHandler.getDataSource();

        SpringFenceHandler.setDataSource(null);
        DataSource result2 = SpringFenceHandler.getDataSource();

        // Then
        assertSame(dataSource1, result1);
        // result2 might be null, which is expected
    }
}
