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
package org.apache.seata.server.session;

import java.io.File;
import java.io.IOException;
import org.apache.seata.common.XID;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.server.store.StoreConfig.SessionMode;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;

import static org.apache.seata.common.Constants.ASYNC_COMMITTING;
import static org.apache.seata.common.Constants.RETRY_COMMITTING;
import static org.apache.seata.common.Constants.RETRY_ROLLBACKING;
import static org.apache.seata.common.Constants.TX_TIMEOUT_CHECK;
import static org.apache.seata.common.Constants.UNDOLOG_DELETE;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static org.apache.seata.server.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;
import static java.io.File.separator;

/**
 * The type Session holder test.
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class SessionHolderTest {
    private String pathname;

    @BeforeEach
    public void before() {
        String sessionStorePath =
            SessionHolder.CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR) + separator
                + XID.getPort();
        //delete file previously created
        pathname = sessionStorePath + File.separator + ROOT_SESSION_MANAGER_NAME;
       // SessionHolder.init(StoreMode.REDIS.getName());
    }

    @Test
    @Order(1)
    public void testInit() throws IOException {
        File rootSessionFile = new File(pathname);
        if (rootSessionFile.exists()) {
            rootSessionFile.delete();
        }
        SessionHolder.init(SessionMode.FILE);
        try {
            final File actual = new File(pathname);
            Assertions.assertTrue(actual.exists());
            Assertions.assertTrue(actual.isFile());
        } finally {
            SessionHolder.destroy();
        }
    }

    @AfterEach
    public void after() {
        final File actual = new File(pathname);
        if (actual.exists()) {
            actual.delete();
        }
    }

//    @Test
    @Order(2)
    public void test_retryRollbackingLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(RETRY_ROLLBACKING));
    }

//    @Test
    @Order(3)
    public void test_unRetryRollbackingLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(RETRY_ROLLBACKING));
    }

//    @Test
    @Order(4)
    public void test_retryCommittingLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(RETRY_COMMITTING));
    }

//    @Test
    @Order(5)
    public void test_unRetryCommittingLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(RETRY_COMMITTING));
    }

//    @Test
    @Order(6)
    public void test_asyncCommittingLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(ASYNC_COMMITTING));
    }

//    @Test
    @Order(7)
    public void test_unAsyncCommittingLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(ASYNC_COMMITTING));
    }

//    @Test
    @Order(8)
    public void test_txTimeoutCheckLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(TX_TIMEOUT_CHECK));
    }

//    @Test
    @Order(9)
    public void test_unTxTimeoutCheckLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(TX_TIMEOUT_CHECK));
    }

//    @Test
    @Order(10)
    public void test_undoLogDeleteLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(UNDOLOG_DELETE));
    }

//    @Test
    @Order(11)
    public void test_unUndoLogDeleteLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(UNDOLOG_DELETE));
    }
}
