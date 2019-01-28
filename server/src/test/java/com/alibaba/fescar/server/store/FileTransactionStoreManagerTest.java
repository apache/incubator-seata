/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.alibaba.fescar.server.store;

import java.util.List;

import com.alibaba.fescar.server.session.FileBasedSessionManager;
import com.alibaba.fescar.server.session.GlobalSession;
import com.alibaba.fescar.server.session.SessionManager;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The type File transaction store manager test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/25
 */
public class FileTransactionStoreManagerTest {
    private FileTransactionStoreManager fileTransactionStoreManager;

    /**
     * Sets up.
     *
     * @throws Exception the exception
     */
    @BeforeClass
    public void setUp() throws Exception {
        SessionManager sessionManager = new FileBasedSessionManager("default", "root.data");
        fileTransactionStoreManager = new FileTransactionStoreManager("root.data", sessionManager);
    }

    /**
     * Tear down.
     */
    @AfterClass
    public void tearDown() {
        fileTransactionStoreManager.shutdown();
    }

    /**
     * Write session test.
     *
     * @param globalSession the global session
     */
    @Test(dataProvider = "sessionProvider")
    public void writeSessionTest(GlobalSession globalSession) {
        boolean result = fileTransactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD,
            globalSession);
        Assert.assertTrue(result);
    }

    /**
     * Read write store from file test.
     *
     * @param globalSession the global session
     */
    @Test(dataProvider = "sessionProvider")
    public void readWriteStoreFromFileTest(GlobalSession globalSession) {
        fileTransactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, globalSession);
        List<TransactionWriteStore> stores = fileTransactionStoreManager.readWriteStoreFromFile(100, false);
        Assert.assertNotNull(stores);
        Assert.assertTrue(stores.size() > 0);
    }

    /**
     * Session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] sessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        return new Object[][] {{globalSession}};
    }

}
