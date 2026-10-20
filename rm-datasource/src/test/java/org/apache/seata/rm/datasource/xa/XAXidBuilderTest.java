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
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Tests for XAXidBuilder
 *
 */
public class XAXidBuilderTest {

    private static final long MOCK_BRANCH_ID = 1582688600006L;
    private static final String MOCK_XID = "127.0.0.1:8091:" + MOCK_BRANCH_ID;

    @Test
    public void testXid() throws Throwable {
        XAXid xaXid = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID);

        XAXid retrievedXAXid = XAXidBuilder.build(xaXid.getGlobalTransactionId(), xaXid.getBranchQualifier());
        String retrievedXid = retrievedXAXid.getGlobalXid();
        long retrievedBranchId = retrievedXAXid.getBranchId();

        Assertions.assertEquals(MOCK_XID, retrievedXid);
        Assertions.assertEquals(MOCK_BRANCH_ID, retrievedBranchId);
    }

    @Test
    public void testLegacyEncodingUnchangedByDbType() {
        XAXid legacy = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID);
        XAXid withNullDbType = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID, null);
        XAXid withMySqlDbType = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID, JdbcConstants.MYSQL);

        for (XAXid xaXid : new XAXid[] {withNullDbType, withMySqlDbType}) {
            Assertions.assertEquals(legacy.getFormatId(), xaXid.getFormatId());
            Assertions.assertArrayEquals(legacy.getGlobalTransactionId(), xaXid.getGlobalTransactionId());
            Assertions.assertArrayEquals(legacy.getBranchQualifier(), xaXid.getBranchQualifier());
            Assertions.assertArrayEquals(MOCK_XID.getBytes(StandardCharsets.UTF_8), xaXid.getGlobalTransactionId());
            Assertions.assertEquals(legacy, xaXid);
        }
    }

    @Test
    public void testPerBranchGtridForDm() {
        XAXid xaXid = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID, JdbcConstants.DM);

        Assertions.assertEquals(XABranchXid.SEATA_XA_XID_FORMAT_ID_PER_BRANCH_GTRID, xaXid.getFormatId());
        Assertions.assertArrayEquals(
                (MOCK_XID + "-" + MOCK_BRANCH_ID).getBytes(StandardCharsets.UTF_8), xaXid.getGlobalTransactionId());
        Assertions.assertArrayEquals(
                ("-" + MOCK_BRANCH_ID).getBytes(StandardCharsets.UTF_8), xaXid.getBranchQualifier());
        // the logical Seata xid and branch id must not be affected by the physical gtrid encoding
        Assertions.assertEquals(MOCK_XID, xaXid.getGlobalXid());
        Assertions.assertEquals(MOCK_BRANCH_ID, xaXid.getBranchId());
        Assertions.assertEquals(MOCK_XID + "-" + MOCK_BRANCH_ID, xaXid.toString());
    }

    @Test
    public void testDmBranchesOwnDistinctGtrids() {
        XAXid branchOne = XAXidBuilder.build(MOCK_XID, 1L, JdbcConstants.DM);
        XAXid branchTwo = XAXidBuilder.build(MOCK_XID, 2L, JdbcConstants.DM);

        // the root cause of the DM restriction: two branches of one global transaction must not
        // share the same (formatId, gtrid) on the database side
        Assertions.assertFalse(
                java.util.Arrays.equals(branchOne.getGlobalTransactionId(), branchTwo.getGlobalTransactionId()));
        Assertions.assertEquals(branchOne.getFormatId(), branchTwo.getFormatId());
        Assertions.assertNotEquals(branchOne, branchTwo);
    }

    @Test
    public void testDmEncodingIsDeterministicAcrossPhaseTwoRebuild() {
        // phase-1: built from the branch registration
        XAXid phaseOne = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID, JdbcConstants.DM);
        // phase-2: rebuilt from the xid and branchId carried by the branch commit/rollback request
        XAXid phaseTwo = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID, JdbcConstants.DM);

        Assertions.assertEquals(phaseOne.getFormatId(), phaseTwo.getFormatId());
        Assertions.assertArrayEquals(phaseOne.getGlobalTransactionId(), phaseTwo.getGlobalTransactionId());
        Assertions.assertArrayEquals(phaseOne.getBranchQualifier(), phaseTwo.getBranchQualifier());
        Assertions.assertEquals(phaseOne, phaseTwo);
    }

    @Test
    public void testPerBranchGtridExceedingXOpenLimitIsRejected() {
        // the X/Open specification limits the gtrid to 64 bytes: build an xid long enough that
        // xid + "-" + branchId exceeds the limit (the legacy encoding of the same xid stays valid)
        StringBuilder longXidBuilder = new StringBuilder("127.0.0.1:8091:");
        while (longXidBuilder.length() < 64) {
            longXidBuilder.append('8');
        }
        String longXid = longXidBuilder.toString();

        Assertions.assertDoesNotThrow(() -> XAXidBuilder.build(longXid, MOCK_BRANCH_ID));
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> XAXidBuilder.build(longXid, MOCK_BRANCH_ID, JdbcConstants.DM));
    }
}
