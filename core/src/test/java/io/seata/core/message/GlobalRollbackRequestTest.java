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
package io.seata.core.message;

import java.nio.ByteBuffer;

import io.seata.core.protocol.transaction.GlobalRollbackRequest;

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
        Assertions.assertEquals("xid=127.0.0.1:8091:1249853,extraData=test_extra_data", globalRollbackRequest.toString());
    }

}