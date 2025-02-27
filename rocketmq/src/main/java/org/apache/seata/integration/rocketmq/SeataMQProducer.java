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

import org.apache.rocketmq.client.Validators;
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
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.rm.DefaultResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Seata MQ Producer
 **/
public class SeataMQProducer extends TransactionMQProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataMQProducer.class);

    private static final List<GlobalStatus> COMMIT_STATUSES =
        Arrays.asList(GlobalStatus.Committed, GlobalStatus.Committing, GlobalStatus.CommitRetrying);
    private static final List<GlobalStatus> ROLLBACK_STATUSES =
        Arrays.asList(GlobalStatus.Rollbacked, GlobalStatus.Rollbacking, GlobalStatus.RollbackRetrying);

    public static String PROPERTY_SEATA_XID = RootContext.KEY_XID;
    public static String PROPERTY_SEATA_BRANCHID = RootContext.KEY_BRANCHID;
    private TransactionListener transactionListener;

    private TCCRocketMQ tccRocketMQ;

    SeataMQProducer(final String producerGroup) {
        this(null, producerGroup, null);
    }

    SeataMQProducer(final String namespace, final String producerGroup, RPCHook rpcHook) {
        super(namespace, producerGroup, rpcHook);
        this.transactionListener = new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                return LocalTransactionState.UNKNOW;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String xid = msg.getProperty(PROPERTY_SEATA_XID);
                if (StringUtils.isBlank(xid)) {
                    LOGGER.error("msg has no xid, msgTransactionId: {}, msg will be rollback", msg.getTransactionId());
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                GlobalStatus globalStatus =
                    DefaultResourceManager.get().getGlobalStatus(SeataMQProducerFactory.ROCKET_BRANCH_TYPE, xid);
                if (COMMIT_STATUSES.contains(globalStatus)) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else if (ROLLBACK_STATUSES.contains(globalStatus) || GlobalStatus.isOnePhaseTimeout(globalStatus)) {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                } else if (GlobalStatus.Finished.equals(globalStatus)) {
                    LOGGER.error("global transaction finished, msg will be rollback, xid: {}", xid);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.UNKNOW;
            }
        };
    }

    @Override
    public SendResult send(Message msg)
        throws MQClientException, MQBrokerException, RemotingException, InterruptedException {
        return send(msg, this.getSendMsgTimeout());
    }

    @Override
    public SendResult send(Message msg, long timeout)
        throws MQClientException, MQBrokerException, RemotingException, InterruptedException {
        if (RootContext.inGlobalTransaction()) {
            if (tccRocketMQ == null) {
                throw new RuntimeException("TCCRocketMQ is not initialized");
            }
            return tccRocketMQ.prepare(msg, timeout);
        } else {
            return super.send(msg, timeout);
        }
    }

    public SendResult doSendMessageInTransaction(final Message msg, long timeout, String xid, long branchId)
        throws MQClientException {
        msg.setTopic(withNamespace(msg.getTopic()));
        if (msg.getDelayTimeLevel() != 0) {
            MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_DELAY_TIME_LEVEL);
        }
        Validators.checkMessage(msg, this);

        SendResult sendResult = null;
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_PRODUCER_GROUP, this.getProducerGroup());
        MessageAccessor.putProperty(msg, PROPERTY_SEATA_XID, xid);
        MessageAccessor.putProperty(msg, PROPERTY_SEATA_BRANCHID, String.valueOf(branchId));
        try {
            sendResult = superSend(msg, timeout);
        } catch (Exception e) {
            throw new MQClientException("send message Exception", e);
        }

        if (SendStatus.SEND_OK != sendResult.getSendStatus()) {
            throw new RuntimeException("Message send fail.status=" + sendResult.getSendStatus());
        }
        if (sendResult.getTransactionId() != null) {
            msg.putUserProperty("__transactionId__", sendResult.getTransactionId());
        }
        String transactionId = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
        if (null != transactionId && !"".equals(transactionId)) {
            msg.setTransactionId(transactionId);
        }
        return sendResult;
    }

    public SendResult superSend(Message msg, long timeout)
        throws MQClientException, MQBrokerException, RemotingException, InterruptedException {
        return super.send(msg, timeout);
    }

    @Override
    public TransactionListener getTransactionListener() {
        return transactionListener;
    }

    public void setTccRocketMQ(TCCRocketMQ tccRocketMQ) {
        this.tccRocketMQ = tccRocketMQ;
    }
}
