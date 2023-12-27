package io.seata.integration.rocketmq;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.rocketmq.client.Validators;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCCRocketMQImpl implements TCCRocketMQ {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCCRocketMQImpl.class);

    private DefaultMQProducer defaultMQProducer;

    @Override
    public void setDefaultMQProducer(DefaultMQProducer defaultMQProducer) {
        this.defaultMQProducer = defaultMQProducer;
    }

    @Override
    public SendResult prepare(BusinessActionContext context, Message message)
            throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        SendResult sendResult = halfSend(defaultMQProducer, message);
        //todo 一致性问题
        LOGGER.info("RocketMQ message send prepare, xid = {}, bid = {}", context.getXid(), context.getBranchId());
        Map<String, Object> params = new HashMap<>(2);
        params.put("sendResult", sendResult);
        BusinessActionContextUtil.addContext(params);
        return sendResult;
    }

    @Override
    public boolean commit(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException {
        Message message = context.getActionContext("message", Message.class);
        SendResult sendResult = context.getActionContext("sendResult", SendResult.class);
        //todo 方法过时
        DefaultMQProducerImpl producerImpl = defaultMQProducer.getDefaultMQProducerImpl();
        producerImpl.endTransaction(message, sendResult, LocalTransactionState.COMMIT_MESSAGE, null);
        LOGGER.info("RocketMQ message send commit, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext context)
            throws UnknownHostException, MQBrokerException, RemotingException, InterruptedException {
        Message message = context.getActionContext("message", Message.class);
        SendResult sendResult = context.getActionContext("sendResult", SendResult.class);
        //todo 方法过时
        DefaultMQProducerImpl producerImpl = defaultMQProducer.getDefaultMQProducerImpl();
        producerImpl.endTransaction(message, sendResult, LocalTransactionState.ROLLBACK_MESSAGE, null);
        LOGGER.info("RocketMQ message send rollback, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }


    public static SendResult halfSend(DefaultMQProducer producer,
                                      Message msg) throws MQClientException {
        // ignore DelayTimeLevel parameter
        if (msg.getDelayTimeLevel() != 0) {
            MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_DELAY_TIME_LEVEL);
        }

        Validators.checkMessage(msg, producer);

        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_PRODUCER_GROUP, producer.getProducerGroup());
        SendResult sendResult;
        try {
            sendResult = producer.send(msg);
        } catch (Exception e) {
            throw new MQClientException("send message Exception", e);
        }

        switch (sendResult.getSendStatus()) {
            case SEND_OK: {
                if (sendResult.getTransactionId() != null) {
                    msg.putUserProperty("__transactionId__", sendResult.getTransactionId());
                }
                String transactionId = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
                if (null != transactionId && !"".equals(transactionId)) {
                    msg.setTransactionId(transactionId);
                }
            }
            break;
            case FLUSH_DISK_TIMEOUT:
            case FLUSH_SLAVE_TIMEOUT:
            case SLAVE_NOT_AVAILABLE:
            default:
                throw new RuntimeException("Message send fail.");
        }
        return sendResult;
    }


}