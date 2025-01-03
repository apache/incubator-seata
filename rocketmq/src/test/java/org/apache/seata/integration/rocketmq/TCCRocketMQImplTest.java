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

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * the type TCCRocketMQImpl
 */
public class TCCRocketMQImplTest {
    @Mock
    private SeataMQProducer producer;

    @Mock
    private DefaultMQProducerImpl producerImpl;
    @Mock
    private BusinessActionContext businessActionContext;

    private TCCRocketMQImpl tccRocketMQ;
    private TCCRocketMQImpl prepareTccRocketMQ;

    private static final String TEST_TOPIC = "testTopic";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        tccRocketMQ = new TCCRocketMQImpl();
        prepareTccRocketMQ = new TCCRocketMQImpl();

        Field producerImplField = TCCRocketMQImpl.class.getDeclaredField("producerImpl");
        producerImplField.setAccessible(true);
        producerImplField.set(tccRocketMQ, producerImpl);
        prepareTccRocketMQ.setProducer(producer);
    }

    @Test
    void testPrepare() throws MQClientException {
        MockedStatic<BusinessActionContextUtil> mockedStatic = mockStatic(BusinessActionContextUtil.class);
        try {

            Message message = new Message(TEST_TOPIC, "testBody".getBytes());
            long timeout = 3000L;
            String xid = "testXid";
            long branchId = 123L;

            mockedStatic.when(BusinessActionContextUtil::getContext).thenReturn(businessActionContext);
            when(businessActionContext.getXid()).thenReturn(xid);
            when(businessActionContext.getBranchId()).thenReturn(branchId);

            SendResult mockSendResult = mockSendResultWithId();
            when(mockSendResult.getSendStatus()).thenReturn(SendStatus.SEND_OK);
            when(producer.doSendMessageInTransaction(message, timeout, xid, branchId)).thenReturn(mockSendResult);

            SendResult result = prepareTccRocketMQ.prepare(message, timeout);

            assertNotNull(result);
            assertEquals(SendStatus.SEND_OK, result.getSendStatus());
            assertEquals(0, message.getDelayTimeLevel());

            verify(producer).doSendMessageInTransaction(message, timeout, xid, branchId);
            mockedStatic.verify(BusinessActionContextUtil::getContext, times(1));
        } finally {
            mockedStatic.close();
        }
    }

    @Test
    void testPrepareWithException() throws MQClientException {
        MockedStatic<BusinessActionContextUtil> mockedStatic = mockStatic(BusinessActionContextUtil.class);
        try {

            Message message = new Message(TEST_TOPIC, "testBody".getBytes());
            long timeout = 3000L;
            String xid = "testXid";
            long branchId = 123L;

            mockedStatic.when(BusinessActionContextUtil::getContext).thenReturn(businessActionContext);
            when(businessActionContext.getXid()).thenReturn(xid);
            when(businessActionContext.getBranchId()).thenReturn(branchId);

            when(producer.doSendMessageInTransaction(message, timeout, xid, branchId)).thenThrow(
                new MQClientException("Test exception", null));

            assertThrows(MQClientException.class, () -> prepareTccRocketMQ.prepare(message, timeout));

            verify(producer).doSendMessageInTransaction(message, timeout, xid, branchId);
            mockedStatic.verify(BusinessActionContextUtil::getContext, times(1));
            mockedStatic.verify(() -> BusinessActionContextUtil.addContext(any()), never());
        } finally {
            mockedStatic.close();
        }
    }

    @Test
    void testCommitSuccess()
        throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TimeoutException,
        TransactionException {

        Message message = new Message(TEST_TOPIC, "testBody".getBytes());
        SendResult sendResult = mockSendResultWithId();

        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(message);
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(sendResult);
        when(businessActionContext.getXid()).thenReturn("testXid");
        when(businessActionContext.getBranchId()).thenReturn(123L);

        boolean result = tccRocketMQ.commit(businessActionContext);

        assertTrue(result);
        verify(producerImpl).endTransaction(eq(message), eq(sendResult), eq(LocalTransactionState.COMMIT_MESSAGE),
            isNull());
    }



    @Test
    void testCommitWithNullMessageOrResult() {

        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(null);
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(
                mock(SendResult.class));
        assertThrows(TransactionException.class, () -> tccRocketMQ.commit(businessActionContext));

        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(new Message());
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(null);
        assertThrows(TransactionException.class, () -> tccRocketMQ.commit(businessActionContext));
    }

    @Test
    void testCommitWithException()
        throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TimeoutException {

        Message message = new Message(TEST_TOPIC, "testBody".getBytes());
        SendResult sendResult = mockSendResultWithId();

        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(message);
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(sendResult);

        doThrow(new MQBrokerException(1, "Test exception")).when(producerImpl)
            .endTransaction(any(), any(), any(), any());

        assertThrows(MQBrokerException.class, () -> tccRocketMQ.commit(businessActionContext));
    }

    @Test
    void testRollbackSuccess()
        throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TransactionException {

        Message message = new Message(TEST_TOPIC, "testBody".getBytes());
        SendResult sendResult = mockSendResultWithId();

        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(message);
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(sendResult);
        when(businessActionContext.getXid()).thenReturn("testXid");
        when(businessActionContext.getBranchId()).thenReturn(123L);

        boolean result = tccRocketMQ.rollback(businessActionContext);

        assertTrue(result);
        verify(producerImpl).endTransaction(eq(message), eq(sendResult), eq(LocalTransactionState.ROLLBACK_MESSAGE),
            isNull());
    }

    @Test
    void testRollbackWithNullMessageOrResult()
        throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TransactionException {

        when(businessActionContext.getXid()).thenReturn("testXid");
        when(businessActionContext.getBranchId()).thenReturn(123L);

        SendResult sendResult = mock(SendResult.class);
        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(null);
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(sendResult);
        boolean result = tccRocketMQ.rollback(businessActionContext);
        assertTrue(result);

        Message message = new Message(TEST_TOPIC, "testBody".getBytes());
        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(message);
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(null);
        boolean result2 = tccRocketMQ.rollback(businessActionContext);
        assertTrue(result2);
    }

    @Test
    void testRollbackWithException()
        throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException {

        Message message = new Message(TEST_TOPIC, "testBody".getBytes());
        SendResult sendResult = mockSendResultWithId();

        when(businessActionContext.getActionContext("ROCKET_MSG", Message.class)).thenReturn(message);
        when(businessActionContext.getActionContext("ROCKET_SEND_RESULT", SendResult.class)).thenReturn(sendResult);
        when(businessActionContext.getXid()).thenReturn("testXid");
        when(businessActionContext.getBranchId()).thenReturn(123L);

        doThrow(new MQBrokerException(1, "Test exception")).when(producerImpl)
            .endTransaction(any(), any(), any(), any());

        assertThrows(MQBrokerException.class, () -> tccRocketMQ.rollback(businessActionContext));
    }


    private static SendResult mockSendResultWithId() {
        SendResult mock = mock(SendResult.class);
        when(mock.getMsgId()).thenReturn("testMsgId");
        when(mock.getOffsetMsgId()).thenReturn("testOffsetMsgId");
        return mock;
    }
}
