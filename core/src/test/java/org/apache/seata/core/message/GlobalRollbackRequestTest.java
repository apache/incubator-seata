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
package org.apache.seata.core.message;

import org.apache.seata.core.protocol.transaction.GlobalRollbackRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Global rollback request test.
 */
public class GlobalRollbackRequestTest {

    /**
     * Test to string.
     */
    @Test
    public void testToString() {
        GlobalRollbackRequest globalRollbackRequest = new GlobalRollbackRequest();
        globalRollbackRequest.setXid("127.0.0.1:8091:1249853");
        globalRollbackRequest.setExtraData("test_extra_data");
        Assertions.assertEquals("GlobalRollbackRequest{xid='127.0.0.1:8091:1249853', extraData='test_extra_data'}", globalRollbackRequest.toString());
    }

}
