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

import io.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for DefaultGlobalTransaction implementation.
 */
public class DefaultGlobalTransactionTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                DefaultGlobalTransaction.class.isAnnotationPresent(Deprecated.class),
                "DefaultGlobalTransaction should be marked as @Deprecated");
    }

    @Test
    public void testImplementsGlobalTransaction() {
        assertTrue(
                GlobalTransaction.class.isAssignableFrom(DefaultGlobalTransaction.class),
                "DefaultGlobalTransaction should implement GlobalTransaction");
    }

    @Test
    public void testDefaultConstructor() {
        DefaultGlobalTransaction transaction = new DefaultGlobalTransaction();
        assertNotNull(transaction);
        assertNotNull(transaction.getInstance());
    }

    @Test
    public void testConstructorWithParameters() {
        String xid = "test-xid-123";
        GlobalStatus status = GlobalStatus.Begin;
        GlobalTransactionRole role = GlobalTransactionRole.Launcher;

        DefaultGlobalTransaction transaction = new DefaultGlobalTransaction(xid, status, role);

        assertNotNull(transaction);
        assertEquals(xid, transaction.getXid());
    }

    @Test
    public void testGetXid() {
        String expectedXid = "192.168.1.1:8091:123456";
        DefaultGlobalTransaction transaction =
                new DefaultGlobalTransaction(expectedXid, GlobalStatus.Begin, GlobalTransactionRole.Launcher);

        assertEquals(expectedXid, transaction.getXid());
    }

    @Test
    public void testGetLocalStatus() {
        DefaultGlobalTransaction transaction =
                new DefaultGlobalTransaction("xid", GlobalStatus.Begin, GlobalTransactionRole.Launcher);

        GlobalStatus localStatus = transaction.getLocalStatus();
        assertNotNull(localStatus);
    }

    @Test
    public void testGetGlobalTransactionRole() {
        DefaultGlobalTransaction launcherTx =
                new DefaultGlobalTransaction("xid1", GlobalStatus.Begin, GlobalTransactionRole.Launcher);
        assertEquals(GlobalTransactionRole.Launcher, launcherTx.getGlobalTransactionRole());

        DefaultGlobalTransaction participantTx =
                new DefaultGlobalTransaction("xid2", GlobalStatus.Begin, GlobalTransactionRole.Participant);
        assertEquals(GlobalTransactionRole.Participant, participantTx.getGlobalTransactionRole());
    }

    @Test
    public void testGetInstance() {
        DefaultGlobalTransaction transaction = new DefaultGlobalTransaction();
        assertNotNull(transaction.getInstance());
        assertTrue(transaction.getInstance() instanceof org.apache.seata.tm.api.DefaultGlobalTransaction);
    }

    @Test
    public void testGlobalTransactionRoleConversion() {
        // Test Launcher role
        DefaultGlobalTransaction launcherTx =
                new DefaultGlobalTransaction("xid1", GlobalStatus.Begin, GlobalTransactionRole.Launcher);
        assertEquals(GlobalTransactionRole.Launcher, launcherTx.getGlobalTransactionRole());

        // Test Participant role
        DefaultGlobalTransaction participantTx =
                new DefaultGlobalTransaction("xid2", GlobalStatus.Begin, GlobalTransactionRole.Participant);
        assertEquals(GlobalTransactionRole.Participant, participantTx.getGlobalTransactionRole());
    }

    @Test
    public void testConstructorWithDifferentStatuses() {
        // Test with different GlobalStatus values
        DefaultGlobalTransaction beginTx =
                new DefaultGlobalTransaction("xid1", GlobalStatus.Begin, GlobalTransactionRole.Launcher);
        assertNotNull(beginTx);

        DefaultGlobalTransaction committedTx =
                new DefaultGlobalTransaction("xid2", GlobalStatus.Committed, GlobalTransactionRole.Launcher);
        assertNotNull(committedTx);

        DefaultGlobalTransaction rollbackedTx =
                new DefaultGlobalTransaction("xid3", GlobalStatus.Rollbacked, GlobalTransactionRole.Launcher);
        assertNotNull(rollbackedTx);
    }

    @Test
    public void testGetCreateTime() {
        DefaultGlobalTransaction transaction =
                new DefaultGlobalTransaction("xid", GlobalStatus.Begin, GlobalTransactionRole.Launcher);
        long createTime = transaction.getCreateTime();
        assertTrue(createTime >= 0, "Create time should be non-negative");
    }
}
