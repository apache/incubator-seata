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
package org.apache.seata.core.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link MessageType}.
 */
public class MessageTypeTest {

    @Test
    public void testVersionNotSupportConstant() {
        assertEquals(-1, MessageType.VERSION_NOT_SUPPORT);
    }

    @Test
    public void testTypeNotExistConstant() {
        assertEquals(0, MessageType.TYPE_NOT_EXIST);
    }

    @Test
    public void testGlobalTransactionTypeConstants() {
        assertEquals(1, MessageType.TYPE_GLOBAL_BEGIN);
        assertEquals(2, MessageType.TYPE_GLOBAL_BEGIN_RESULT);
        assertEquals(7, MessageType.TYPE_GLOBAL_COMMIT);
        assertEquals(8, MessageType.TYPE_GLOBAL_COMMIT_RESULT);
        assertEquals(9, MessageType.TYPE_GLOBAL_ROLLBACK);
        assertEquals(10, MessageType.TYPE_GLOBAL_ROLLBACK_RESULT);
        assertEquals(15, MessageType.TYPE_GLOBAL_STATUS);
        assertEquals(16, MessageType.TYPE_GLOBAL_STATUS_RESULT);
        assertEquals(17, MessageType.TYPE_GLOBAL_REPORT);
        assertEquals(18, MessageType.TYPE_GLOBAL_REPORT_RESULT);
        assertEquals(21, MessageType.TYPE_GLOBAL_LOCK_QUERY);
        assertEquals(22, MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT);
    }

    @Test
    public void testBranchTransactionTypeConstants() {
        assertEquals(3, MessageType.TYPE_BRANCH_COMMIT);
        assertEquals(4, MessageType.TYPE_BRANCH_COMMIT_RESULT);
        assertEquals(5, MessageType.TYPE_BRANCH_ROLLBACK);
        assertEquals(6, MessageType.TYPE_BRANCH_ROLLBACK_RESULT);
        assertEquals(11, MessageType.TYPE_BRANCH_REGISTER);
        assertEquals(12, MessageType.TYPE_BRANCH_REGISTER_RESULT);
        assertEquals(13, MessageType.TYPE_BRANCH_STATUS_REPORT);
        assertEquals(14, MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT);
    }

    @Test
    public void testMergeTypeConstants() {
        assertEquals(59, MessageType.TYPE_SEATA_MERGE);
        assertEquals(60, MessageType.TYPE_SEATA_MERGE_RESULT);
    }

    @Test
    public void testRegistrationTypeConstants() {
        assertEquals(101, MessageType.TYPE_REG_CLT);
        assertEquals(102, MessageType.TYPE_REG_CLT_RESULT);
        assertEquals(103, MessageType.TYPE_REG_RM);
        assertEquals(104, MessageType.TYPE_REG_RM_RESULT);
    }

    @Test
    public void testOtherTypeConstants() {
        assertEquals(111, MessageType.TYPE_RM_DELETE_UNDOLOG);
        assertEquals(120, MessageType.TYPE_HEARTBEAT_MSG);
        assertEquals(121, MessageType.TYPE_BATCH_RESULT_MSG);
    }
}
