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

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.rm.tcc.api.BusinessActionContext;

import java.net.UnknownHostException;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * The interface Tcc rocket mq.
 */
public interface TCCRocketMQ {

    /**
     * set SeataMQProducer
     *
     * @param producer the producer
     */
    void setProducer(SeataMQProducer producer);

    /**
     * RocketMQ half send
     *
     * @param message  the message
     * @param timeout  the timeout
     * @return SendResult
     */
    SendResult prepare(Message message, long timeout) throws MQClientException;

    /**
     * RocketMQ half send commit
     *
     * @param context the BusinessActionContext
     * @return SendResult
     * @throws UnknownHostException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws InterruptedException
     */
    boolean commit(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TransactionException;

    /**
     * RocketMQ half send rollback
     *
     * @param context the BusinessActionContext
     * @return
     * @throws UnknownHostException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws InterruptedException
     */
    boolean rollback(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TransactionException;


}
