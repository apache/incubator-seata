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

import io.seata.rm.tcc.api.BusinessActionContext;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCCRocketMQImpl implements TCCRocketMQ {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCCRocketMQImpl.class);

    private final ConcurrentHashMap<String, Map<String, Object>> map = new ConcurrentHashMap<>();

    @Override
    public SendResult prepare(BusinessActionContext context, DefaultMQProducer defaultMQProducer, Message message)
        throws MQBrokerException, RemotingException, NoSuchFieldException, InterruptedException, MQClientException {
        String key = context.getXid() + context.getBranchId();
        SendResult sendResult = RocketMQUtils.halfSend(defaultMQProducer, message);
        LOGGER.info("RocketMQ message send prepare, xid = {}, bid = {}", context.getXid(), context.getBranchId());
        Map<String, Object> params = new HashMap<>(3);
        params.put("defaultMQProducer", defaultMQProducer);
        params.put("message", message);
        params.put("sendResult", sendResult);
        map.put(key, params);
        return sendResult;
    }

    @Override
    public boolean commit(BusinessActionContext context)
        throws UnknownHostException, MQBrokerException, RemotingException, NoSuchFieldException, InterruptedException {
        String key = context.getXid() + context.getBranchId();
        Map<String, Object> params = map.get(key);
        DefaultMQProducer producer = (DefaultMQProducer) params.get("defaultMQProducer");
        Message message = (Message) params.get("message");
        SendResult sendResult = (SendResult) params.get("sendResult");
        RocketMQUtils.confirm(producer, message, sendResult);
        map.remove(key);
        LOGGER.info("RocketMQ message send commit, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext context)
        throws UnknownHostException, MQBrokerException, RemotingException, NoSuchFieldException, InterruptedException {
        String key = context.getXid() + context.getBranchId();
        Map<String, Object> params = map.get(key);
        DefaultMQProducer producer = (DefaultMQProducer) params.get("defaultMQProducer");
        Message message = (Message) params.get("message");
        SendResult sendResult = (SendResult) params.get("sendResult");
        RocketMQUtils.cancel(producer, message, sendResult);
        LOGGER.info("RocketMQ message send rollback, xid = {}, branchId = {}", context.getXid(), context.getBranchId());
        return true;
    }

}
