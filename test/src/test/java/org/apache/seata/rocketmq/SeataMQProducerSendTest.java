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
package org.apache.seata.rocketmq;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.mockserver.ProtocolTestConstants;
import org.apache.seata.core.rpc.netty.mockserver.TmClientTest;
import org.apache.seata.integration.rocketmq.SeataMQProducer;
import org.apache.seata.integration.rocketmq.SeataMQProducerFactory;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.tm.DefaultTransactionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * seata mq producer test
 **/
@Tag("excludeFromCI")
public class SeataMQProducerSendTest {

    @BeforeAll
    public static void before() {
        MockServer.start();
        // should start mq server here
    }

    @AfterAll
    public static void after() {
        MockServer.close();
    }

    @Test
    public void testSendCommit() throws MQBrokerException, RemotingException, InterruptedException, MQClientException, TransactionException {
        TransactionManager tm = TmClientTest.getTm();

        SeataMQProducer producer = SeataMQProducerFactory.createSingle("yourIp:9876", "test");
        producer.send(new Message("yourTopic", "testMessage".getBytes(StandardCharsets.UTF_8)));

        tm.commit(RootContext.getXID());
    }

    @Test
    public void testSendRollback() throws MQBrokerException, RemotingException, InterruptedException, MQClientException, TransactionException {
        TransactionManager tm = TmClientTest.getTm();

        SeataMQProducer producer = SeataMQProducerFactory.createSingle("yourIp:9876", "test");
        producer.send(new Message("yourTopic", "testMessage".getBytes(StandardCharsets.UTF_8)));

        tm.rollback(RootContext.getXID());
    }


    private static TransactionManager getTmAndBegin() throws TransactionException {
        String app = ProtocolTestConstants.APPLICATION_ID;
        String group = ProtocolTestConstants.SERVICE_GROUP;
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(app, group);
        tmNettyRemotingClient.init();
        TransactionManager tm = new DefaultTransactionManager();
        String xid = tm.begin(app, group, "test", 60000);
        RootContext.bind(xid);
        return tm;
    }
}
