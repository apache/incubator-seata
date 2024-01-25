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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit test for {@link BatchResultMessage}.
 *
 */
class BatchResultMessageTest {

    @Test
    void getTypeCode() {
        BatchResultMessage batchResultMessage = new BatchResultMessage();

        Assertions.assertEquals(MessageType.TYPE_BATCH_RESULT_MSG, batchResultMessage.getTypeCode());
    }

    @Test
    void getResultMessages() {
        BatchResultMessage batchResultMessage = new BatchResultMessage();

        Assertions.assertTrue(batchResultMessage.getResultMessages().isEmpty());
    }

    @Test
    void setResultMessages() {
        BatchResultMessage batchResultMessage = new BatchResultMessage();

        List<AbstractResultMessage> resultMessages = Arrays.asList(new RegisterTMResponse(), new RegisterRMResponse(false));
        batchResultMessage.setResultMessages(resultMessages);

        Assertions.assertIterableEquals(resultMessages, batchResultMessage.getResultMessages());
    }

    @Test
    void getMsgIds() {
        BatchResultMessage batchResultMessage = new BatchResultMessage();

        Assertions.assertTrue(batchResultMessage.getMsgIds().isEmpty());
    }

    @Test
    void setMsgIds() {
        BatchResultMessage batchResultMessage = new BatchResultMessage();

        List<Integer> msgIds = Arrays.asList(1, 2, 3);
        batchResultMessage.setMsgIds(msgIds);

        Assertions.assertIterableEquals(msgIds, batchResultMessage.getMsgIds());
    }
}
