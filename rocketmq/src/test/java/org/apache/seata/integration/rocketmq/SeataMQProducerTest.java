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
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * seata mq producer test
 **/
public class SeataMQProducerTest {

    @Mock
    private TransactionMQProducer transactionMQProducer;


    @InjectMocks
    private SeataMQProducer producer;

    @BeforeEach
    void setUp()  {
        producer = Mockito.spy(new SeataMQProducer("testGroup"));

    }

    @Test
    public void testCreate() {
        new SeataMQProducer("testProducerGroup");
        new SeataMQProducer("testNamespace", "testProducerGroup", null);
    }

    @Test
    void getTransactionListener_ShouldReturnNonNullTransactionListener() {
        TransactionListener transactionListener = producer.getTransactionListener();
        assertNotNull(transactionListener, "TransactionListener should not be null");
    }

    @Test
    void testSend() throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        // Arrange
        Message msg = new Message("testTopic", "testBody".getBytes());
        SendResult expectedResult = mock(SendResult.class);
        int expectedTimeout = 3000; // 假设默认超时时间是3000毫秒

        doReturn(expectedTimeout).when(producer).getSendMsgTimeout();
        doReturn(expectedResult).when(producer).send(any(Message.class), anyLong());

        // Act
        SendResult result = producer.send(msg);

        // Assert
        assertSame(expectedResult, result);
        verify(producer).send(msg, expectedTimeout);
    }

    @Test
    void testSendWithException() throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        // Arrange
        Message msg = new Message("testTopic", "testBody".getBytes());
        int expectedTimeout = 3000;

        doReturn(expectedTimeout).when(producer).getSendMsgTimeout();
        doThrow(new MQClientException("Test exception", null))
            .when(producer).send(any(Message.class), anyInt());

        // Act & Assert
        assertThrows(MQClientException.class, () -> producer.send(msg));
        verify(producer).send(msg, expectedTimeout);
    }

}
