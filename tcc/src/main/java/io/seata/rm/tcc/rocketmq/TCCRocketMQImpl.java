package io.seata.rm.tcc.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.rm.tcc.api.BusinessActionContext;

public class TCCRocketMQImpl implements TCCRocketMQ {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCCRocketMQImpl.class);

    private final ConcurrentHashMap<String, Map<String, Object>> map = new ConcurrentHashMap<>();

    @Override
    public boolean prepare(BusinessActionContext context, Map<String, Object> params) {
        String key = context.getXid() + context.getBranchId();
        DefaultMQProducer producer = (DefaultMQProducer) params.get("defaultMQProducer");
        Message message = (Message) params.get("message");
        SendResult sendResult = MQUtils.halfSend(producer, message);
        params.put("sendResult", sendResult);
        LOGGER.info("rocketmq send prepare");
        map.put(key, params);
        return true;
    }

    @Override
    public boolean commit(BusinessActionContext context) {
        String key = context.getXid() + context.getBranchId();
        Map<String, Object> params = map.get(key);
        DefaultMQProducer producer = (DefaultMQProducer) params.get("defaultMQProducer");
        Message message = (Message) params.get("message");
        SendResult sendResult = (SendResult) params.get("sendResult");
        MQUtils.confirm(producer, message, sendResult);
        map.remove(key);
        LOGGER.info("RocketMQ send commit, xid = [{}], branchId = [{}]", context.getXid(), context.getBranchId());
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext context) {
        String key = context.getXid() + context.getBranchId();
        Map<String, Object> params = map.get(key);
        DefaultMQProducer producer = (DefaultMQProducer) params.get("defaultMQProducer");
        Message message = (Message) params.get("message");
        SendResult sendResult = (SendResult) params.get("sendResult");
        MQUtils.cancel(producer, message, sendResult);
        map.remove(key);
        LOGGER.info("RocketMQ send commit, xid = [{}], branchId = [{}]", context.getXid(), context.getBranchId());
        return true;
    }
}
