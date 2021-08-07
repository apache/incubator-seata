package io.seata.rm.tcc.rocketmq;

import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.exception.RequestTimeoutException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionExecuter;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.RequestCallback;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.client.trace.TraceDispatcher;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class RocketMQProducerProxy extends DefaultMQProducer{

    private DefaultMQProducer target;

    public RocketMQProducerProxy(DefaultMQProducer target) {
        this.target = target;
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
        return target.send(msg);
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

    @Override
    public void setCallbackExecutor(ExecutorService callbackExecutor) {
        target.setCallbackExecutor(callbackExecutor);
    }

    @Override
    public void setAsyncSenderExecutor(ExecutorService asyncSenderExecutor) {
        target.setAsyncSenderExecutor(asyncSenderExecutor);
    }

    @Override
    public String getProducerGroup() {
        return target.getProducerGroup();
    }

    @Override
    public void setProducerGroup(String producerGroup) {
        target.setProducerGroup(producerGroup);
    }

    @Override
    public String getCreateTopicKey() {
        return target.getCreateTopicKey();
    }

    @Override
    public void setCreateTopicKey(String createTopicKey) {
        target.setCreateTopicKey(createTopicKey);
    }

    @Override
    public int getSendMsgTimeout() {
        return target.getSendMsgTimeout();
    }

    @Override
    public void setSendMsgTimeout(int sendMsgTimeout) {
        target.setSendMsgTimeout(sendMsgTimeout);
    }

    @Override
    public int getCompressMsgBodyOverHowmuch() {
        return target.getCompressMsgBodyOverHowmuch();
    }

    @Override
    public void setCompressMsgBodyOverHowmuch(int compressMsgBodyOverHowmuch) {
        target.setCompressMsgBodyOverHowmuch(compressMsgBodyOverHowmuch);
    }

    @Override
    public DefaultMQProducerImpl getDefaultMQProducerImpl() {
        return target.getDefaultMQProducerImpl();
    }

    @Override
    public boolean isRetryAnotherBrokerWhenNotStoreOK() {
        return target.isRetryAnotherBrokerWhenNotStoreOK();
    }

    @Override
    public void setRetryAnotherBrokerWhenNotStoreOK(boolean retryAnotherBrokerWhenNotStoreOK) {
        target.setRetryAnotherBrokerWhenNotStoreOK(retryAnotherBrokerWhenNotStoreOK);
    }

    @Override
    public int getMaxMessageSize() {
        return target.getMaxMessageSize();
    }

    @Override
    public void setMaxMessageSize(int maxMessageSize) {
        target.setMaxMessageSize(maxMessageSize);
    }

    @Override
    public int getDefaultTopicQueueNums() {
        return target.getDefaultTopicQueueNums();
    }

    @Override
    public void setDefaultTopicQueueNums(int defaultTopicQueueNums) {
        target.setDefaultTopicQueueNums(defaultTopicQueueNums);
    }

    @Override
    public int getRetryTimesWhenSendFailed() {
        return target.getRetryTimesWhenSendFailed();
    }

    @Override
    public void setRetryTimesWhenSendFailed(int retryTimesWhenSendFailed) {
        target.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
    }

    @Override
    public boolean isSendMessageWithVIPChannel() {
        return target.isSendMessageWithVIPChannel();
    }

    @Override
    public void setSendMessageWithVIPChannel(boolean sendMessageWithVIPChannel) {
        target.setSendMessageWithVIPChannel(sendMessageWithVIPChannel);
    }

    @Override
    public long[] getNotAvailableDuration() {
        return target.getNotAvailableDuration();
    }

    @Override
    public void setNotAvailableDuration(long[] notAvailableDuration) {
        target.setNotAvailableDuration(notAvailableDuration);
    }

    @Override
    public long[] getLatencyMax() {
        return target.getLatencyMax();
    }

    @Override
    public void setLatencyMax(long[] latencyMax) {
        target.setLatencyMax(latencyMax);
    }

    @Override
    public boolean isSendLatencyFaultEnable() {
        return target.isSendLatencyFaultEnable();
    }

    @Override
    public void setSendLatencyFaultEnable(boolean sendLatencyFaultEnable) {
        target.setSendLatencyFaultEnable(sendLatencyFaultEnable);
    }

    @Override
    public int getRetryTimesWhenSendAsyncFailed() {
        return target.getRetryTimesWhenSendAsyncFailed();
    }

    @Override
    public void setRetryTimesWhenSendAsyncFailed(int retryTimesWhenSendAsyncFailed) {
        target.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
    }

    @Override
    public TraceDispatcher getTraceDispatcher() {
        return target.getTraceDispatcher();
    }
}
