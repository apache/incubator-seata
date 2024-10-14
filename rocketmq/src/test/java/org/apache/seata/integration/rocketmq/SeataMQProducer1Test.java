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
package org.apache.seata.integration.rocketmq;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * seata mq producer test
 **/
@ExtendWith(MockitoExtension.class)
public class SeataMQProducer1Test {
    private SeataMQProducer seataMQProducer;

    @BeforeEach
    void setUp() {
        seataMQProducer = spy(new SeataMQProducer("testGroup"));
    }

    @Test
    void testDoSendMessageInTransaction_Success() throws Exception {
        // Arrange
        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        SendResult mockSendResult = new SendResult();
        mockSendResult.setSendStatus(SendStatus.SEND_OK);
        mockSendResult.setTransactionId("testTransactionId");

        doReturn(mockSendResult).when(seataMQProducer).superSend(any(Message.class), anyLong());

        // Act
        SendResult result = seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId);

        // Assert
        assertNotNull(result);
        assertEquals(SendStatus.SEND_OK, result.getSendStatus());
        assertEquals("testTransactionId", msg.getUserProperty("__transactionId__"));
        assertEquals("true", msg.getProperty(MessageConst.PROPERTY_TRANSACTION_PREPARED));
        assertEquals(seataMQProducer.getProducerGroup(), msg.getProperty(MessageConst.PROPERTY_PRODUCER_GROUP));
        assertEquals(xid, msg.getProperty(SeataMQProducer.PROPERTY_SEATA_XID));
        assertEquals(String.valueOf(branchId), msg.getProperty(SeataMQProducer.PROPERTY_SEATA_BRANCHID));

        verify(seataMQProducer).superSend(msg, timeout);
    }

    @Test
    void testDoSendMessageInTransaction_SendException() throws Exception {
        // Arrange
        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        doThrow(new RuntimeException("Send failed")).when(seataMQProducer).superSend(any(Message.class), anyLong());

        // Act & Assert
        assertThrows(MQClientException.class, () ->
            seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId)
        );

        verify(seataMQProducer).superSend(msg, timeout);
    }

    @Test
    void testDoSendMessageInTransaction_SendStatusNotOk() throws Exception {
        // Arrange
        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        SendResult mockSendResult = new SendResult();
        mockSendResult.setSendStatus(SendStatus.FLUSH_DISK_TIMEOUT);

        doReturn(mockSendResult).when(seataMQProducer).superSend(any(Message.class), anyLong());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId)
        );

        verify(seataMQProducer).superSend(msg, timeout);
    }

    @Test
    void testDoSendMessageInTransaction_WithTransactionId() throws Exception {
        // Arrange
        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        SendResult mockSendResult = new SendResult();
        mockSendResult.setSendStatus(SendStatus.SEND_OK);
        mockSendResult.setTransactionId("testTransactionId");

        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX, "clientTransactionId");

        doReturn(mockSendResult).when(seataMQProducer).superSend(any(Message.class), anyLong());

        // Act
        SendResult result = seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId);

        // Assert
        assertNotNull(result);
        assertEquals(SendStatus.SEND_OK, result.getSendStatus());
        assertEquals("testTransactionId", msg.getUserProperty("__transactionId__"));
        assertEquals("clientTransactionId", msg.getTransactionId());

        verify(seataMQProducer).superSend(msg, timeout);
    }

}
