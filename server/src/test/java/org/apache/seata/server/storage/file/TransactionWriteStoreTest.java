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
package org.apache.seata.server.storage.file;

import org.apache.seata.common.XID;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransactionWriteStoreTest extends BaseSpringBootTest {

    @Test
    public void testConstructorWithParameters() {
        GlobalSession session = createGlobalSession();
        TransactionWriteStore store = new TransactionWriteStore(session, LogOperation.GLOBAL_ADD);

        Assertions.assertNotNull(store);
        Assertions.assertEquals(session, store.getSessionRequest());
        Assertions.assertEquals(LogOperation.GLOBAL_ADD, store.getOperate());
    }

    @Test
    public void testDefaultConstructor() {
        TransactionWriteStore store = new TransactionWriteStore();
        Assertions.assertNotNull(store);
        Assertions.assertNull(store.getSessionRequest());
        Assertions.assertNull(store.getOperate());
    }

    @Test
    public void testSettersAndGetters() {
        TransactionWriteStore store = new TransactionWriteStore();
        GlobalSession session = createGlobalSession();

        store.setSessionRequest(session);
        store.setOperate(LogOperation.GLOBAL_UPDATE);

        Assertions.assertEquals(session, store.getSessionRequest());
        Assertions.assertEquals(LogOperation.GLOBAL_UPDATE, store.getOperate());
    }

    @Test
    public void testEncode() {
        GlobalSession session = createGlobalSession();
        TransactionWriteStore store = new TransactionWriteStore(session, LogOperation.GLOBAL_ADD);

        byte[] encoded = store.encode();
        Assertions.assertNotNull(encoded);
        Assertions.assertTrue(encoded.length > 0);
    }

    private GlobalSession createGlobalSession() {
        GlobalSession session = GlobalSession.createGlobalSession("test-app", "test-group", "test-tx", 60000);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setStatus(GlobalStatus.Begin);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("test-data");
        return session;
    }
}
