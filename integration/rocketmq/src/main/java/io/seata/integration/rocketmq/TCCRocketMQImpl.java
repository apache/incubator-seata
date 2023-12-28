package io.seata.integration.rocketmq;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TCCRocketMQImpl implements TCCRocketMQ {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCCRocketMQImpl.class);

    private DefaultMQProducerImpl producerImpl;

    String ROCKET_MSG_KEY = "ROCKET_MSG";
    String ROCKET_SEND_RESULT_KEY = "ROCKET_SEND_RESULT";

    private ConcurrentHashMap resultMap = new ConcurrentHashMap();


    @Override
    public void setDefaultMQProducer(DefaultMQProducer defaultMQProducer) {
        this.producerImpl = new DefaultMQProducerImpl(defaultMQProducer);
    }

    @Override
    public void prepare(BusinessActionContext context, Message message,SendResult sendResult){
        LOGGER.info("RocketMQ message send prepare, xid = {}", BusinessActionContextUtil.getContext().getXid());
        Map<String, Object> params = new HashMap<>(2);
        params.put(ROCKET_MSG_KEY, message);
        params.put(ROCKET_SEND_RESULT_KEY, sendResult);
        BusinessActionContextUtil.addContext(params);
    }

    @Override
    public boolean commit(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException {
        Message message = context.getActionContext(ROCKET_MSG_KEY, Message.class);
        SendResult sendResult = context.getActionContext("sendResult", SendResult.class);
        producerImpl.endTransaction(message, sendResult, LocalTransactionState.COMMIT_MESSAGE, null);
        LOGGER.info("RocketMQ message send commit, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException {
        Message message = context.getActionContext(ROCKET_MSG_KEY, Message.class);
        SendResult sendResult = context.getActionContext("sendResult", SendResult.class);
        producerImpl.endTransaction(message, sendResult, LocalTransactionState.ROLLBACK_MESSAGE, null);
        LOGGER.info("RocketMQ message send rollback, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }

}