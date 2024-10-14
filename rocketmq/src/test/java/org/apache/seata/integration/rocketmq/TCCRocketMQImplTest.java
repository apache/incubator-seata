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
import static org.mockito.ArgumentMatchers.any;
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
    private BusinessActionContext businessActionContext;

    private TCCRocketMQImpl tccRocketMQ;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tccRocketMQ = new TCCRocketMQImpl();
        tccRocketMQ.setProducer(producer);
    }
    @Test
    void testPrepare() throws MQClientException {
        MockedStatic<BusinessActionContextUtil> mockedStatic = mockStatic(BusinessActionContextUtil.class);
        try {
            // Arrange
            Message message = new Message("testTopic", "testBody".getBytes());
            long timeout = 3000L;
            String xid = "testXid";
            long branchId = 123L;

            mockedStatic.when(BusinessActionContextUtil::getContext).thenReturn(businessActionContext);
            when(businessActionContext.getXid()).thenReturn(xid);
            when(businessActionContext.getBranchId()).thenReturn(branchId);

            SendResult mockSendResult = mock(SendResult.class);
            when(mockSendResult.getSendStatus()).thenReturn(SendStatus.SEND_OK);
            when(producer.doSendMessageInTransaction(message, timeout, xid, branchId)).thenReturn(mockSendResult);

            // Act
            SendResult result = tccRocketMQ.prepare(message, timeout);

            // Assert
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
            // Arrange
            Message message = new Message("testTopic", "testBody".getBytes());
            long timeout = 3000L;
            String xid = "testXid";
            long branchId = 123L;

            mockedStatic.when(BusinessActionContextUtil::getContext).thenReturn(businessActionContext);
            when(businessActionContext.getXid()).thenReturn(xid);
            when(businessActionContext.getBranchId()).thenReturn(branchId);

            when(producer.doSendMessageInTransaction(message, timeout, xid, branchId))
                .thenThrow(new MQClientException("Test exception", null));

            // Act & Assert
            assertThrows(MQClientException.class, () -> tccRocketMQ.prepare(message, timeout));

            verify(producer).doSendMessageInTransaction(message, timeout, xid, branchId);
            mockedStatic.verify(BusinessActionContextUtil::getContext, times(1));
            mockedStatic.verify(() -> BusinessActionContextUtil.addContext(any()), never());
        } finally {
            mockedStatic.close();
        }
    }
}
