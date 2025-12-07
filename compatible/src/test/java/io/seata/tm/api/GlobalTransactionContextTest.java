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
package io.seata.tm.api;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for GlobalTransactionContext.
 */
public class GlobalTransactionContextTest {

    @AfterEach
    public void cleanup() {
        RootContext.unbind();
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                GlobalTransactionContext.class.isAnnotationPresent(Deprecated.class),
                "GlobalTransactionContext should be marked as @Deprecated");
    }

    @Test
    public void testCreateNew() {
        GlobalTransaction transaction = GlobalTransactionContext.createNew();

        assertNotNull(transaction, "createNew should return a non-null transaction");
        assertTrue(
                transaction instanceof DefaultGlobalTransaction,
                "createNew should return a DefaultGlobalTransaction instance");
    }

    @Test
    public void testGetCurrentWithNoContext() {
        RootContext.unbind();

        GlobalTransaction transaction = GlobalTransactionContext.getCurrent();

        assertNull(transaction, "getCurrent should return null when no XID is bound");
    }

    @Test
    public void testGetCurrentWithContext() {
        String testXid = "192.168.1.1:8091:123456";
        RootContext.bind(testXid);

        GlobalTransaction transaction = GlobalTransactionContext.getCurrent();

        assertNotNull(transaction, "getCurrent should return a transaction when XID is bound");
        assertEquals(testXid, transaction.getXid(), "Transaction XID should match the bound XID");
        assertEquals(GlobalStatus.Begin, transaction.getLocalStatus(), "Transaction status should be Begin");
        assertEquals(
                GlobalTransactionRole.Participant,
                transaction.getGlobalTransactionRole(),
                "Transaction role should be Participant");
    }

    @Test
    public void testGetCurrentOrCreateWithNoContext() {
        RootContext.unbind();

        GlobalTransaction transaction = GlobalTransactionContext.getCurrentOrCreate();

        assertNotNull(transaction, "getCurrentOrCreate should return a non-null transaction");
        assertTrue(
                transaction instanceof DefaultGlobalTransaction,
                "getCurrentOrCreate should return a DefaultGlobalTransaction instance");
    }

    @Test
    public void testGetCurrentOrCreateWithContext() {
        String testXid = "192.168.1.1:8091:654321";
        RootContext.bind(testXid);

        GlobalTransaction transaction = GlobalTransactionContext.getCurrentOrCreate();

        assertNotNull(transaction, "getCurrentOrCreate should return a non-null transaction");
        assertEquals(testXid, transaction.getXid(), "Transaction XID should match the bound XID");
    }

    @Test
    public void testReload() throws TransactionException {
        String testXid = "192.168.1.1:8091:999999";

        GlobalTransaction transaction = GlobalTransactionContext.reload(testXid);

        assertNotNull(transaction, "reload should return a non-null transaction");
        assertEquals(testXid, transaction.getXid(), "Transaction XID should match the provided XID");
        assertEquals(
                GlobalStatus.UnKnown, transaction.getLocalStatus(), "Reloaded transaction status should be UnKnown");
        assertEquals(
                GlobalTransactionRole.Launcher,
                transaction.getGlobalTransactionRole(),
                "Reloaded transaction role should be Launcher");
    }

    @Test
    public void testReloadedTransactionCannotBegin() throws TransactionException {
        String testXid = "192.168.1.1:8091:888888";

        GlobalTransaction transaction = GlobalTransactionContext.reload(testXid);

        assertThrows(
                IllegalStateException.class,
                () -> transaction.begin(30000, "test"),
                "Reloaded transaction should not allow begin operation");
    }

    @Test
    public void testMultipleCreateNew() {
        GlobalTransaction tx1 = GlobalTransactionContext.createNew();
        GlobalTransaction tx2 = GlobalTransactionContext.createNew();

        assertNotNull(tx1);
        assertNotNull(tx2);
    }

    @Test
    public void testGetCurrentAfterUnbind() {
        String testXid = "192.168.1.1:8091:111111";
        RootContext.bind(testXid);

        GlobalTransaction tx1 = GlobalTransactionContext.getCurrent();
        assertNotNull(tx1);

        RootContext.unbind();

        GlobalTransaction tx2 = GlobalTransactionContext.getCurrent();
        assertNull(tx2, "getCurrent should return null after unbind");
    }

    @Test
    public void testGetCurrentOrCreateMultipleTimes() {
        RootContext.unbind();

        GlobalTransaction tx1 = GlobalTransactionContext.getCurrentOrCreate();
        assertNotNull(tx1);

        // Bind an XID
        String testXid = "192.168.1.1:8091:222222";
        RootContext.bind(testXid);

        GlobalTransaction tx2 = GlobalTransactionContext.getCurrentOrCreate();
        assertNotNull(tx2);
        assertEquals(testXid, tx2.getXid());

        // Unbind and call again
        RootContext.unbind();

        GlobalTransaction tx3 = GlobalTransactionContext.getCurrentOrCreate();
        assertNotNull(tx3);
    }

    @Test
    public void testReloadWithDifferentXids() throws TransactionException {
        String xid1 = "192.168.1.1:8091:100001";
        String xid2 = "192.168.1.1:8091:100002";

        GlobalTransaction tx1 = GlobalTransactionContext.reload(xid1);
        GlobalTransaction tx2 = GlobalTransactionContext.reload(xid2);

        assertNotNull(tx1);
        assertNotNull(tx2);
        assertEquals(xid1, tx1.getXid());
        assertEquals(xid2, tx2.getXid());
    }
}
