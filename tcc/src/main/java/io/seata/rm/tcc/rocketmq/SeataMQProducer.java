/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.tcc.rocketmq;

import io.seata.core.context.RootContext;
import java.util.Collection;
import java.util.List;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.exception.RequestTimeoutException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionExecuter;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.RequestCallback;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeataMQProducer implements MQProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataMQProducer.class);

    private final DefaultMQProducer target;
    private final TCCRocketMQ tccRocketMQ;

    public SeataMQProducer(DefaultMQProducer target, TCCRocketMQ tccRocketMQ) {
        this.target = target;
        this.tccRocketMQ = tccRocketMQ;
    }

    @Override
    public void start() throws MQClientException {
        target.start();
    }

    @Override
    public void shutdown() {
        target.shutdown();
    }

    @Override
    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws MQClientException {
        return target.fetchPublishMessageQueues(topic);
    }

    @Override
    public SendResult send(
        Message msg) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        if (RootContext.inGlobalTransaction()) {
            LOGGER.info("DefaultMQProducer send is in Global Transaction, send() will be proxy");
            return tccRocketMQ.prepare(null, msg);
        } else {
            LOGGER.info("Not in Global Transaction, send() will be proxy");
            return target.send(msg);
        }
    }

    @Override
    public SendResult send(Message msg,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msg, timeout);
    }

    @Override
    public void send(Message msg,
        SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        target.send(msg, sendCallback);
    }

    @Override
    public void send(Message msg, SendCallback sendCallback,
        long timeout) throws MQClientException, RemotingException, InterruptedException {
        target.send(msg, sendCallback, timeout);
    }

    @Override
    public void sendOneway(Message msg) throws MQClientException, RemotingException, InterruptedException {
        target.sendOneway(msg);
    }

    @Override
    public SendResult send(Message msg,
        MessageQueue mq) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msg, mq);
    }

    @Override
    public SendResult send(Message msg, MessageQueue mq,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msg, mq, timeout);
    }

    @Override
    public void send(Message msg, MessageQueue mq,
        SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        target.send(msg, mq, sendCallback);
    }

    @Override
    public void send(Message msg, MessageQueue mq, SendCallback sendCallback,
        long timeout) throws MQClientException, RemotingException, InterruptedException {
        target.send(msg, mq, sendCallback, timeout);
    }

    @Override
    public void sendOneway(Message msg,
        MessageQueue mq) throws MQClientException, RemotingException, InterruptedException {
        target.sendOneway(msg, mq);
    }

    @Override
    public SendResult send(Message msg, MessageQueueSelector selector,
        Object arg) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msg, selector, arg);
    }

    @Override
    public SendResult send(Message msg, MessageQueueSelector selector, Object arg,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msg, selector, arg, timeout);
    }

    @Override
    public void send(Message msg, MessageQueueSelector selector, Object arg,
        SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        target.send(msg, selector, arg, sendCallback);
    }

    @Override
    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback,
        long timeout) throws MQClientException, RemotingException, InterruptedException {
        target.send(msg, selector, arg, sendCallback, timeout);
    }

    @Override
    public Message request(Message msg,
        long timeout) throws RequestTimeoutException, MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.request(msg, timeout);
    }

    @Override
    public void request(Message msg, RequestCallback requestCallback,
        long timeout) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {
        target.request(msg, requestCallback, timeout);
    }

    @Override
    public Message request(Message msg, MessageQueueSelector selector, Object arg,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException, RequestTimeoutException {
        return target.request(msg, selector, arg, timeout);
    }

    @Override
    public void request(Message msg, MessageQueueSelector selector, Object arg, RequestCallback requestCallback,
        long timeout) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {
        target.request(msg, selector, arg, requestCallback, timeout);
    }

    @Override
    public Message request(Message msg, MessageQueue mq,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException, RequestTimeoutException {
        return target.request(msg, mq, timeout);
    }

    @Override
    public void request(Message msg, MessageQueue mq, RequestCallback requestCallback,
        long timeout) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {
        target.request(msg, mq, requestCallback, timeout);
    }

    @Override
    public void sendOneway(Message msg, MessageQueueSelector selector,
        Object arg) throws MQClientException, RemotingException, InterruptedException {
        target.sendOneway(msg, selector, arg);
    }

    @Override
    public TransactionSendResult sendMessageInTransaction(Message msg, LocalTransactionExecuter tranExecuter,
        Object arg) throws MQClientException {
        return target.sendMessageInTransaction(msg, tranExecuter, arg);
    }

    @Override
    public TransactionSendResult sendMessageInTransaction(Message msg, Object arg) throws MQClientException {
        return target.sendMessageInTransaction(msg, arg);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        target.createTopic(key, newTopic, queueNum);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        target.createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        return target.searchOffset(mq, timestamp);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws MQClientException {
        return target.maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws MQClientException {
        return target.minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        return target.earliestMsgStoreTime(mq);
    }

    @Override
    public MessageExt viewMessage(
        String offsetMsgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return target.viewMessage(offsetMsgId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin,
        long end) throws MQClientException, InterruptedException {
        return target.queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    public MessageExt viewMessage(String topic,
        String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return target.viewMessage(topic, msgId);
    }

    @Override
    public SendResult send(
        Collection<Message> msgs) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msgs);
    }

    @Override
    public SendResult send(Collection<Message> msgs,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msgs, timeout);
    }

    @Override
    public SendResult send(Collection<Message> msgs,
        MessageQueue messageQueue) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msgs, messageQueue);
    }

    @Override
    public SendResult send(Collection<Message> msgs, MessageQueue messageQueue,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return target.send(msgs, messageQueue, timeout);
    }

    @Override
    public void send(Collection<Message> msgs,
        SendCallback sendCallback) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        target.send(msgs, sendCallback);
    }

    @Override
    public void send(Collection<Message> msgs, SendCallback sendCallback,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        target.send(msgs, sendCallback, timeout);
    }

    @Override
    public void send(Collection<Message> msgs, MessageQueue mq,
        SendCallback sendCallback) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        target.send(msgs, mq, sendCallback);
    }

    @Override
    public void send(Collection<Message> msgs, MessageQueue mq, SendCallback sendCallback,
        long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        target.send(msgs, mq, sendCallback, timeout);
    }
}
