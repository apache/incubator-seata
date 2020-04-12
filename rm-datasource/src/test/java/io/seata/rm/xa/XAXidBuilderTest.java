/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.rm.xa;

import io.seata.rm.datasource.xa.XAXid;
import io.seata.rm.datasource.xa.XAXidBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for XAXidBuilder
 *
 * @author sharajava
 */
public class XAXidBuilderTest {

    @Test
    public void testXid() throws Throwable {
        long mockBranchId = 1582688600006L;
        String mockXid = "127.0.0.1:8091:" + mockBranchId;
        XAXid xaXid = XAXidBuilder.build(mockXid, mockBranchId);

        XAXid retrievedXAXid = XAXidBuilder.build(xaXid.getGlobalTransactionId(), xaXid.getBranchQualifier());
        String retrievedXid = retrievedXAXid.getGlobalXid();
        long retrievedBranchId = retrievedXAXid.getBranchId();

        Assertions.assertEquals(mockXid, retrievedXid);
        Assertions.assertEquals(mockBranchId, retrievedBranchId);

    }
}
