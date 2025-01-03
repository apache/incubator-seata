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

import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * the type TCCRocketMQImpl
 */
@LocalTCC
public class TCCRocketMQImpl implements TCCRocketMQ {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCCRocketMQImpl.class);
    private static final String ROCKET_MSG_KEY = "ROCKET_MSG";
    private static final String ROCKET_SEND_RESULT_KEY = "ROCKET_SEND_RESULT";

    private SeataMQProducer producer;
    private DefaultMQProducerImpl producerImpl;

    @Override
    public void setProducer(SeataMQProducer producer) {
        this.producer = producer;
        this.producerImpl = producer.getDefaultMQProducerImpl();
    }

    @Override
    @TwoPhaseBusinessAction(name = SeataMQProducerFactory.ROCKET_TCC_NAME)
    public SendResult prepare(Message message, long timeout) throws MQClientException {
        BusinessActionContext context = BusinessActionContextUtil.getContext();
        LOGGER.info("RocketMQ message send prepare, xid = {}", context.getXid());
        Map<String, Object> params = new HashMap<>(8);
        SendResult sendResult = producer.doSendMessageInTransaction(message, timeout, context.getXid(), context.getBranchId());
        message.setDeliverTimeMs(0);
        params.put(ROCKET_MSG_KEY, message);
        params.put(ROCKET_SEND_RESULT_KEY, sendResult);
        BusinessActionContextUtil.addContext(params);
        return sendResult;
    }

    @Override
    public boolean commit(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TransactionException {
        Message message = context.getActionContext(ROCKET_MSG_KEY, Message.class);
        SendResult sendResult = context.getActionContext(ROCKET_SEND_RESULT_KEY, SendResult.class);
        if (checkMqStatus(message, sendResult)) {
            throw new TransactionException("TCCRocketMQ commit but cannot find message or sendResult");
        }
        this.producerImpl.endTransaction(message, sendResult, LocalTransactionState.COMMIT_MESSAGE, null);
        LOGGER.info("RocketMQ message send commit, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException, TransactionException {
        Message message = context.getActionContext(ROCKET_MSG_KEY, Message.class);
        SendResult sendResult = context.getActionContext(ROCKET_SEND_RESULT_KEY, SendResult.class);
        if (checkMqStatus(message, sendResult)) {
            LOGGER.error("TCCRocketMQ rollback but cannot find message or sendResult");
            return true;
        }
        this.producerImpl.endTransaction(message, sendResult, LocalTransactionState.ROLLBACK_MESSAGE, null);
        LOGGER.info("RocketMQ message send rollback, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }

    private static boolean checkMqStatus(Message message, SendResult sendResult) {
        boolean empty = message == null || sendResult == null ||
                (StringUtils.isBlank(sendResult.getOffsetMsgId()) && StringUtils.isBlank(sendResult.getMsgId()));
        if (empty) {
            LOGGER.info("checkMqStatus message = {}, sendResult = {}", message, sendResult);
        }
        return empty;
    }
}
