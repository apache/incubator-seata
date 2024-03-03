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

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.core.rpc.netty.mockserver.ProtocolTestConstants;
import org.apache.seata.core.rpc.netty.mockserver.TmClientTest;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.rm.RMClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * seata mq producer test
 **/
public class SeataMQProducerSendTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataMQProducerSendTest.class);


    public static String TOPIC = "Topic--AA";
    public static String NAME_SERVER = "10.213.3.25:9876";

    @BeforeAll
    public static void before() throws MQClientException {
        MockServer.start();
        // should start mq server here
    }

    @AfterAll
    public static void after() {
        MockServer.close();
    }

    @Test
    @Disabled
    public void testSendCommit() throws MQBrokerException, RemotingException, InterruptedException, MQClientException, TransactionException {
        TransactionManager tm = getTmAndBegin();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MQPushConsumer consumer = startConsume(countDownLatch);
        SeataMQProducer producer = SeataMQProducerFactory.createSingle(NAME_SERVER, "test");
        producer.send(new Message(TOPIC, "testMessage".getBytes(StandardCharsets.UTF_8)));

        tm.commit(RootContext.getXID());
        LOGGER.info("global commit");
        boolean await = countDownLatch.await(2, TimeUnit.SECONDS);
        LOGGER.info("await:{}", await);
        producer.shutdown();
        consumer.shutdown();
    }

    @Test
    @Disabled
    public void testSendRollback()
        throws MQBrokerException, RemotingException, InterruptedException, MQClientException, TransactionException {
        TransactionManager tm = getTmAndBegin();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MQPushConsumer consumer = startConsume(countDownLatch);
        SeataMQProducer producer = SeataMQProducerFactory.createSingle(NAME_SERVER, "test");
        producer.send(new Message(TOPIC, "testMessage".getBytes(StandardCharsets.UTF_8)));

        tm.rollback(RootContext.getXID());
        LOGGER.info("global rollback");
        try {
            boolean await = countDownLatch.await(2, TimeUnit.SECONDS);
            LOGGER.info("await:{}", await);
        } catch (Exception e) {
            Assertions.assertEquals(e.getClass(), InterruptedException.class);
        } finally {
            producer.shutdown();
            consumer.shutdown();
        }
    }


    private static MQPushConsumer startConsume(CountDownLatch countDownLatch) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("yourGroup");
        consumer.setNamesrvAddr(NAME_SERVER);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe(TOPIC,"*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msg, context) -> {
            LOGGER.info("%s Receive New Messages: {} {}", Thread.currentThread().getName(), msg);
            countDownLatch.countDown();
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        return consumer;
    }


    private static TransactionManager getTmAndBegin() throws TransactionException {
        TransactionManager tm = TmClientTest.getTm();
        RMClient.init(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP);
        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP, "testRocket", 60000);
        RootContext.bind(xid);
        return tm;
    }
}
