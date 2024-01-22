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
package io.seata.integration.rocketmq;

import com.google.common.collect.Sets;
import io.seata.core.context.RootContext;
import io.seata.integration.tx.api.interceptor.InvocationWrapper;
import io.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import io.seata.integration.tx.api.interceptor.handler.AbstractProxyInvocationHandler;
import org.apache.rocketmq.client.Validators;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionExecuter;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionCheckListener;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.protocol.NamespaceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * RocketMQ Interceptor Handler
 */
public class RocketMQInterceptorHandler extends AbstractProxyInvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQInterceptorHandler.class);

    private TransactionMQProducer producer;

    public RocketMQInterceptorHandler(TransactionMQProducer producer) {
        this.producer = producer;
        TransactionListener transactionListener = producer.getTransactionListener();
        TransactionCheckListener checkListener = producer.getTransactionCheckListener();
        if (transactionListener != null) {
            LOGGER.warn("TransactionListener will be wrapped by SeataTransactionListener");
            this.producer.setTransactionListener(new SeataTransactionListener(transactionListener));
        }
        if (checkListener != null) {
            LOGGER.warn("TransactionCheckListener will be wrapped by SeataTransactionCheckListener");
            this.producer.setTransactionCheckListener(new SeataTransactionCheckListener(checkListener));
        }
    }

    @Override
    protected Object doInvoke(InvocationWrapper invocation) throws Throwable {
        Message msg = null;
        LocalTransactionExecuter tranExecuter = null;
        Object arg = null;
        try {
            Object[] arguments = invocation.getArguments();
            if (arguments.length == 2) {
                msg = (Message) arguments[0];
                arg = arguments[1];
                if (null == producer.getTransactionListener()) {
                    throw new MQClientException("TransactionListener is null", null);
                }
            } else if (arguments.length == 3) {
                msg = (Message) arguments[0];
                tranExecuter = (LocalTransactionExecuter) arguments[1];
                arg = arguments[2];
                if (null == producer.getTransactionCheckListener()) {
                    throw new MQClientException("localTransactionBranchCheckListener is null", null);
                }
            } else {
                throw new UnsupportedOperationException("unsupported method, can not be proxy");
            }
        } catch (Exception e) {
            LOGGER.warn("covert arguments error", e);
            return invocation.proceed();
        }

        msg.setTopic(NamespaceUtil.wrapNamespace(producer.getNamespace(), msg.getTopic()));
        return doSendMessageInTransaction(msg, tranExecuter, arg, producer);
    }


    private TransactionSendResult doSendMessageInTransaction(
            Message msg, LocalTransactionExecuter tranExecuter, Object arg, DefaultMQProducer producer) throws MQClientException {
        TransactionListener transactionListener = producer.getDefaultMQProducerImpl().getCheckListener();
        if (null == tranExecuter && null == transactionListener) {
            throw new MQClientException("tranExecutor is null", null);
        }

        // ignore DelayTimeLevel parameter
        if (msg.getDelayTimeLevel() != 0) {
            MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_DELAY_TIME_LEVEL);
        }

        Validators.checkMessage(msg, producer);

        SendResult sendResult = null;
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_PRODUCER_GROUP, producer.getProducerGroup());
        try {
            if (RootContext.inGlobalTransaction()) {
                tccRocketMQ.prepare();
            }
            sendResult = producer.send(msg);
        } catch (Exception e) {
            throw new MQClientException("send message Exception", e);
        }

        LocalTransactionState localTransactionState = LocalTransactionState.UNKNOW;
        Throwable localException = null;
        switch (sendResult.getSendStatus()) {
            case SEND_OK: {
                try {
                    if (sendResult.getTransactionId() != null) {
                        msg.putUserProperty("__transactionId__", sendResult.getTransactionId());
                    }
                    if (RootContext.inGlobalTransaction()) {
                        msg.putUserProperty(SeataRocketMQConst.PROP_KEY_IN_GLOBAL_TRANSACTION, "true");
                    }
                    String transactionId = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
                    if (null != transactionId && !"".equals(transactionId)) {
                        msg.setTransactionId(transactionId);
                    }

                    if (null != tranExecuter) {
                        localTransactionState = tranExecuter.executeLocalTransactionBranch(msg, arg);
                    } else if (transactionListener != null) {
                        LOGGER.debug("Used new transaction API");
                        localTransactionState = transactionListener.executeLocalTransaction(msg, arg);
                    }

                    if (null == localTransactionState) {
                        localTransactionState = LocalTransactionState.UNKNOW;
                    }

                    if (localTransactionState != LocalTransactionState.COMMIT_MESSAGE) {
                        LOGGER.warn("executeLocalTransactionBranch return {}", localTransactionState);
                        LOGGER.info(msg.toString());
                    }

                    if (RootContext.inGlobalTransaction()) {
                        if (localTransactionState == LocalTransactionState.UNKNOW) {
                            LOGGER.warn("seata global transaction fail cause executeLocalTransactionBranch return UNKNOW," +
                                    "localTransactionState will be mark as ROLLBACK_MESSAGE");
                            localTransactionState = LocalTransactionState.ROLLBACK_MESSAGE;
                        } else {
                            tccRocketMQ.report();
                            LOGGER.info("executeLocalTransactionBranch state=COMMIT_MESSAGE, but global transaction not complete,return UNKNOW");
                            localTransactionState = LocalTransactionState.UNKNOW;
                        }
                    }
                } catch (Throwable e) {
                    LOGGER.error("executeLocalTransactionBranch exception", e);
                    localException = e;
                }
            }
            break;
            case FLUSH_DISK_TIMEOUT:
            case FLUSH_SLAVE_TIMEOUT:
            case SLAVE_NOT_AVAILABLE:
                localTransactionState = LocalTransactionState.ROLLBACK_MESSAGE;
                break;
            default:
                break;
        }

        try {
            producer.getDefaultMQProducerImpl().endTransaction(msg, sendResult, localTransactionState, localException);
        } catch (Exception e) {
            LOGGER.warn("local transaction execute " + localTransactionState + ", but end broker transaction failed", e);
        }

        TransactionSendResult transactionSendResult = new TransactionSendResult();
        transactionSendResult.setSendStatus(sendResult.getSendStatus());
        transactionSendResult.setMessageQueue(sendResult.getMessageQueue());
        transactionSendResult.setMsgId(sendResult.getMsgId());
        transactionSendResult.setQueueOffset(sendResult.getQueueOffset());
        transactionSendResult.setTransactionId(sendResult.getTransactionId());
        transactionSendResult.setLocalTransactionState(localTransactionState);
        return transactionSendResult;
    }


    @Override
    public Set<String> getMethodsToProxy() {
        return Sets.newHashSet("sendMessageInTransaction");
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return SeataInterceptorPosition.Any;
    }

}
