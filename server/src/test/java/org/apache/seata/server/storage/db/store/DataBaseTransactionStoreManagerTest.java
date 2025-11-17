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
package org.apache.seata.server.storage.db.store;

import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "dbCaseEnabled", matches = "true")
public class DataBaseTransactionStoreManagerTest extends BaseSpringBootTest {

    private DataBaseTransactionStoreManager dataBaseTransactionStoreManager;

    @BeforeEach
    public void setUp() {
        dataBaseTransactionStoreManager = DataBaseTransactionStoreManager.getInstance();
    }

    @Test
    public void testGetInstance() {
        Assertions.assertNotNull(dataBaseTransactionStoreManager);
    }

    @Test
    public void testGetInstanceSingleton() {
        DataBaseTransactionStoreManager instance1 = DataBaseTransactionStoreManager.getInstance();
        DataBaseTransactionStoreManager instance2 = DataBaseTransactionStoreManager.getInstance();

        Assertions.assertNotNull(instance1);
        Assertions.assertNotNull(instance2);
        Assertions.assertEquals(instance1, instance2);
    }
}
