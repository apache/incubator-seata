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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for XABranchXid
 */
public class XABranchXidTest {
    @Test
    void testEquals() throws Exception {
        XABranchXid xid1 = new XABranchXid("xid1", 1);
        XABranchXid xid2 = new XABranchXid("xid1", 1);
        XABranchXid xid3 = new XABranchXid("xid2", 2);
        XABranchXid xid4 = null;

        Assertions.assertEquals(xid1, xid2);
        Assertions.assertNotEquals(xid1, xid3);
        Assertions.assertNotEquals(xid1, xid4);
        Assertions.assertNotEquals(xid1, new Object());
    }

    @Test
    void testHashCode() throws Exception {
        XABranchXid xid1 = new XABranchXid("xid1", 1);
        XABranchXid xid2 = new XABranchXid("xid1", 1);
        XABranchXid xid3 = new XABranchXid("xid2", 2);

        Assertions.assertEquals(xid1.hashCode(), xid2.hashCode());
        Assertions.assertNotEquals(xid1.hashCode(), xid3.hashCode());
    }
}
