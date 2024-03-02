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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * seata mq producer test
 **/
@Tag("excludeCI")
public class SeataMQProducerSendTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataMQProducerSendTest.class);


    public static String TOPIC = "yourTopic";
    public static String NAME_SERVER = "yourIp:9876";

    @BeforeAll
    public static void before() throws MQClientException {
        MockServer.start();
        startConsume();
        // should start mq server here
    }

    @AfterAll
    public static void after() {
        MockServer.close();
    }

//    @Test
    public void testSendCommit() throws MQBrokerException, RemotingException, InterruptedException, MQClientException, TransactionException {
        TransactionManager tm = getTmAndBegin();

        SeataMQProducer producer = SeataMQProducerFactory.createSingle(NAME_SERVER, "test");
        producer.send(new Message(TOPIC, "testMessage".getBytes(StandardCharsets.UTF_8)));

        Thread.sleep(2000);
        tm.commit(RootContext.getXID());
        LOGGER.info("commit ok");
        Thread.sleep(2000);
    }

//    @Test
    public void testSendRollback() throws MQBrokerException, RemotingException, InterruptedException, MQClientException, TransactionException {
        TransactionManager tm = getTmAndBegin();

        SeataMQProducer producer = SeataMQProducerFactory.createSingle(NAME_SERVER, "test");
        producer.send(new Message(TOPIC, "testMessage".getBytes(StandardCharsets.UTF_8)));

        Thread.sleep(2000);
        tm.rollback(RootContext.getXID());
        LOGGER.info("rollback ok");
        Thread.sleep(2000);
    }


    private static MQPushConsumer startConsume() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("yourGroup");
        consumer.setNamesrvAddr(NAME_SERVER);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe(TOPIC,"*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msg, context) -> {
            System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msg);
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
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
