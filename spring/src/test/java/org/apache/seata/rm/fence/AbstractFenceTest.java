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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base class for fence tests to handle static field cleanup and prevent test pollution
 */
public abstract class AbstractFenceTest {

    private DataSource originalDataSource;
    private TransactionTemplate originalTransactionTemplate;
    private LinkedBlockingQueue<?> originalLogQueue;

    @BeforeEach
    public void setUpBase() throws Exception {
        // Save original static state
        originalDataSource = SpringFenceHandler.getDataSource();

        // Save original LOG_QUEUE state (for cleanup purposes)
        Field logQueueField = SpringFenceHandler.class.getDeclaredField("LOG_QUEUE");
        logQueueField.setAccessible(true);
        originalLogQueue = (LinkedBlockingQueue<?>) logQueueField.get(null);

        // Clear the queue to avoid interference between tests
        if (originalLogQueue != null) {
            originalLogQueue.clear();
        }
    }

    @AfterEach
    public void tearDownBase() throws Exception {
        // Restore original static state to prevent test pollution
        SpringFenceHandler.setDataSource(originalDataSource);
        SpringFenceHandler.setTransactionTemplate(originalTransactionTemplate);

        // Clear the queue again after test execution
        if (originalLogQueue != null) {
            originalLogQueue.clear();
        }
    }

    /**
     * Template method for subclasses to implement their own setup logic
     */
    protected void doSetUp() throws Exception {
        // Override in subclasses if needed
    }

    /**
     * Template method for subclasses to implement their own teardown logic
     */
    protected void doTearDown() throws Exception {
        // Override in subclasses if needed
    }
}
