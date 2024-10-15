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

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.rm.DefaultResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * seata mq producer test
 **/
public class SeataMQProducerTest {

    @Mock
    private TransactionMQProducer transactionMQProducer;

    private TCCRocketMQ tccRocketMQ;
    @InjectMocks
    private SeataMQProducer producer;
    private SeataMQProducer producerTwo;
    private SeataMQProducer seataMQProducer;
    private TransactionListener transactionListener;

    @BeforeEach
    void setUp() {
        producer = Mockito.spy(new SeataMQProducer("testGroup"));
        seataMQProducer = spy(new SeataMQProducer("testGroup"));
        tccRocketMQ = mock(TCCRocketMQImpl.class);
        producer.setTccRocketMQ(tccRocketMQ);
        producerTwo = new SeataMQProducer("namespace", "producerGroup", null);
        transactionListener = producerTwo.getTransactionListener();
    }

    @Test
    public void testCreate() {
        new SeataMQProducer("testProducerGroup");
        new SeataMQProducer("testNamespace", "testProducerGroup", null);
    }

    @Test
    void testExecuteLocalTransaction() {
        Message msg = new Message();
        assertEquals(LocalTransactionState.UNKNOW, transactionListener.executeLocalTransaction(msg, null));
    }

    @Test
    void testCheckLocalTransactionWithNoXid() {
        MessageExt msg = new MessageExt();
        msg.setTransactionId("testTransactionId");
        assertEquals(LocalTransactionState.ROLLBACK_MESSAGE, transactionListener.checkLocalTransaction(msg));
    }

    @Test
    void testCheckLocalTransactionWithCommitStatus() {
        MessageExt msg = new MessageExt();
        msg.putUserProperty(SeataMQProducer.PROPERTY_SEATA_XID, "testXid");

        try (MockedStatic<DefaultResourceManager> mockedStatic = mockStatic(DefaultResourceManager.class)) {
            DefaultResourceManager mockResourceManager = mock(DefaultResourceManager.class);
            mockedStatic.when(DefaultResourceManager::get).thenReturn(mockResourceManager);
            when(mockResourceManager.getGlobalStatus(SeataMQProducerFactory.ROCKET_BRANCH_TYPE, "testXid")).thenReturn(
                GlobalStatus.Committed);

            assertEquals(LocalTransactionState.COMMIT_MESSAGE, transactionListener.checkLocalTransaction(msg));
        }
    }

    @Test
    void testCheckLocalTransactionWithRollbackStatus() {
        MessageExt msg = new MessageExt();
        msg.putUserProperty(SeataMQProducer.PROPERTY_SEATA_XID, "testXid");

        try (MockedStatic<DefaultResourceManager> mockedStatic = mockStatic(DefaultResourceManager.class)) {
            DefaultResourceManager mockResourceManager = mock(DefaultResourceManager.class);
            mockedStatic.when(DefaultResourceManager::get).thenReturn(mockResourceManager);
            when(mockResourceManager.getGlobalStatus(SeataMQProducerFactory.ROCKET_BRANCH_TYPE, "testXid")).thenReturn(
                GlobalStatus.Rollbacked);

            assertEquals(LocalTransactionState.ROLLBACK_MESSAGE, transactionListener.checkLocalTransaction(msg));
        }
    }

    @Test
    void testCheckLocalTransactionWithFinishedStatus() {
        MessageExt msg = new MessageExt();
        msg.putUserProperty(SeataMQProducer.PROPERTY_SEATA_XID, "testXid");

        try (MockedStatic<DefaultResourceManager> mockedStatic = mockStatic(DefaultResourceManager.class)) {
            DefaultResourceManager mockResourceManager = mock(DefaultResourceManager.class);
            mockedStatic.when(DefaultResourceManager::get).thenReturn(mockResourceManager);
            when(mockResourceManager.getGlobalStatus(SeataMQProducerFactory.ROCKET_BRANCH_TYPE, "testXid")).thenReturn(
                GlobalStatus.Finished);

            assertEquals(LocalTransactionState.ROLLBACK_MESSAGE, transactionListener.checkLocalTransaction(msg));
        }
    }

    @Test
    void testCheckLocalTransactionWithUnknownStatus() {
        MessageExt msg = new MessageExt();
        msg.putUserProperty(SeataMQProducer.PROPERTY_SEATA_XID, "testXid");

        try (MockedStatic<DefaultResourceManager> mockedStatic = mockStatic(DefaultResourceManager.class)) {
            DefaultResourceManager mockResourceManager = mock(DefaultResourceManager.class);
            mockedStatic.when(DefaultResourceManager::get).thenReturn(mockResourceManager);
            when(mockResourceManager.getGlobalStatus(SeataMQProducerFactory.ROCKET_BRANCH_TYPE, "testXid")).thenReturn(
                GlobalStatus.Begin);

            assertEquals(LocalTransactionState.UNKNOW, transactionListener.checkLocalTransaction(msg));
        }
    }

    @Test
    void testSendWithoutGlobalTransaction()
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        Message msg = new Message("testTopic", "testBody".getBytes());
        long timeout = 3000L;
        SendResult expectedResult = mock(SendResult.class);

        doReturn(expectedResult).when(producer).send(msg, timeout);

        SendResult result = producer.send(msg, timeout);

        assertSame(expectedResult, result);
        verify(producer).send(msg, timeout);
        verifyNoInteractions(tccRocketMQ);
    }

    @Test
    void testSendWithGlobalTransaction()
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        Message msg = new Message("testTopic", "testBody".getBytes());
        long timeout = 3000L;
        SendResult expectedResult = mock(SendResult.class);

        RootContext.bind("DummyXID");
        try {
            when(tccRocketMQ.prepare(msg, timeout)).thenReturn(expectedResult);

            SendResult result = producer.send(msg, timeout);

            assertSame(expectedResult, result);
            verify(tccRocketMQ).prepare(msg, timeout);
        } finally {
            RootContext.unbind();
        }
    }

    @Test
    void testSendWithGlobalTransactionAndNullTCCRocketMQ() {
        Message msg = new Message("testTopic", "testBody".getBytes());
        long timeout = 3000L;

        producer.setTccRocketMQ(null);
        RootContext.bind("DummyXID");
        try {
            assertThrows(RuntimeException.class, () -> producer.send(msg, timeout));
        } finally {
            RootContext.unbind();
        }
    }

    @Test
    void testSend() throws MQClientException, RemotingException, MQBrokerException, InterruptedException {

        Message msg = new Message("testTopic", "testBody".getBytes());
        SendResult expectedResult = mock(SendResult.class);
        int expectedTimeout = 3000;

        doReturn(expectedTimeout).when(producer).getSendMsgTimeout();
        doReturn(expectedResult).when(producer).send(any(Message.class), anyLong());

        SendResult result = producer.send(msg);

        assertSame(expectedResult, result);
        verify(producer).send(msg, expectedTimeout);
    }

    @Test
    void testSendWithException() throws MQClientException, RemotingException, MQBrokerException, InterruptedException {

        Message msg = new Message("testTopic", "testBody".getBytes());
        int expectedTimeout = 3000;

        doReturn(expectedTimeout).when(producer).getSendMsgTimeout();
        doThrow(new MQClientException("Test exception", null)).when(producer).send(any(Message.class), anyInt());

        assertThrows(MQClientException.class, () -> producer.send(msg));
        verify(producer).send(msg, expectedTimeout);
    }

    @Test
    void testDoSendMessageInTransactionWithNonOkStatus() throws Exception {

        Message msg = new Message("testTopic", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        SendResult mockSendResult = mock(SendResult.class);
        when(mockSendResult.getSendStatus()).thenReturn(SendStatus.FLUSH_DISK_TIMEOUT);

        doReturn(mockSendResult).when(producer).send(any(Message.class), anyLong());

        assertThrows(MQClientException.class, () -> producer.doSendMessageInTransaction(msg, timeout, xid, branchId));
    }

    @Test
    void testDoSendMessageInTransactionWithException() throws Exception {

        Message msg = new Message("testTopic", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        doThrow(new RuntimeException("Test exception")).when(producer).send(any(Message.class), anyLong());
        doCallRealMethod().when(producer)
            .doSendMessageInTransaction(any(Message.class), anyLong(), anyString(), anyLong());

        assertThrows(MQClientException.class, () -> producer.doSendMessageInTransaction(msg, timeout, xid, branchId));
    }

    @Test
    void testDoSendMessageInTransactionSuccess() throws Exception {

        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        SendResult mockSendResult = new SendResult();
        mockSendResult.setSendStatus(SendStatus.SEND_OK);
        mockSendResult.setTransactionId("testTransactionId");

        doReturn(mockSendResult).when(seataMQProducer).superSend(any(Message.class), anyLong());

        SendResult result = seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId);

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
    void testDoSendMessageInTransactionSendException() throws Exception {

        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        doThrow(new RuntimeException("Send failed")).when(seataMQProducer).superSend(any(Message.class), anyLong());

        assertThrows(MQClientException.class,
            () -> seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId));

        verify(seataMQProducer).superSend(msg, timeout);
    }

    @Test
    void testDoSendMessageInTransactionSendStatusNotOk() throws Exception {

        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        SendResult mockSendResult = new SendResult();
        mockSendResult.setSendStatus(SendStatus.FLUSH_DISK_TIMEOUT);

        doReturn(mockSendResult).when(seataMQProducer).superSend(any(Message.class), anyLong());

        assertThrows(RuntimeException.class,
            () -> seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId));

        verify(seataMQProducer).superSend(msg, timeout);
    }

    @Test
    void testDoSendMessageInTransactionWithTransactionId() throws Exception {

        Message msg = new Message("testTopic", "testTag", "testKey", "testBody".getBytes());
        long timeout = 3000L;
        String xid = "testXid";
        long branchId = 123L;

        SendResult mockSendResult = new SendResult();
        mockSendResult.setSendStatus(SendStatus.SEND_OK);
        mockSendResult.setTransactionId("testTransactionId");

        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX, "clientTransactionId");

        doReturn(mockSendResult).when(seataMQProducer).superSend(any(Message.class), anyLong());

        SendResult result = seataMQProducer.doSendMessageInTransaction(msg, timeout, xid, branchId);

        assertNotNull(result);
        assertEquals(SendStatus.SEND_OK, result.getSendStatus());
        assertEquals("testTransactionId", msg.getUserProperty("__transactionId__"));
        assertEquals("clientTransactionId", msg.getTransactionId());

        verify(seataMQProducer).superSend(msg, timeout);
    }

    @Test
    void getTransactionListenerShouldReturnNonNullTransactionListener() {
        TransactionListener transactionListener = producer.getTransactionListener();
        assertNotNull(transactionListener, "TransactionListener should not be null");
    }

}
