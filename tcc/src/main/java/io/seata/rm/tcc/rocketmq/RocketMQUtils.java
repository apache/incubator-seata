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

import io.seata.common.util.ReflectionUtil;
import java.net.UnknownHostException;
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

public class RocketMQUtils {
    private static final String PRODUCER_IMPL = "defaultMQProducerImpl";

    public static SendResult halfSend(DefaultMQProducer defaultMQProducer,
        Message msg) throws NoSuchFieldException, MQClientException {
        // ignore DelayTimeLevel parameter
        if (msg.getDelayTimeLevel() != 0) {
            MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_DELAY_TIME_LEVEL);
        }

        Validators.checkMessage(msg, defaultMQProducer);

        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_PRODUCER_GROUP, defaultMQProducer.getProducerGroup());
        DefaultMQProducerImpl defaultMQProducerImpl = ReflectionUtil.getFieldValue(defaultMQProducer, PRODUCER_IMPL);
        SendResult sendResult = null;
        try {
            sendResult = defaultMQProducerImpl.send(msg);
        } catch (Exception e) {
            throw new MQClientException("send message Exception", e);
        }

        switch (sendResult.getSendStatus()) {
            case FLUSH_DISK_TIMEOUT:
            case FLUSH_SLAVE_TIMEOUT:
            case SLAVE_NOT_AVAILABLE:
                throw new RuntimeException("Message send fail.");
            default:
                break;
        }
        return sendResult;
    }

    public static void confirm(DefaultMQProducer defaultMQProducer, Message msg,
        SendResult sendResult) throws NoSuchFieldException, UnknownHostException, MQBrokerException, RemotingException, InterruptedException {
        DefaultMQProducerImpl defaultMQProducerImpl = ReflectionUtil.getFieldValue(defaultMQProducer, PRODUCER_IMPL);
        defaultMQProducerImpl.endTransaction(msg, sendResult, LocalTransactionState.COMMIT_MESSAGE, null);
    }

    public static void cancel(DefaultMQProducer defaultMQProducer, Message msg,
        SendResult sendResult) throws NoSuchFieldException, UnknownHostException, MQBrokerException, RemotingException, InterruptedException {
        DefaultMQProducerImpl defaultMQProducerImpl = ReflectionUtil.getFieldValue(defaultMQProducer, PRODUCER_IMPL);
        defaultMQProducerImpl.endTransaction(msg, sendResult, LocalTransactionState.ROLLBACK_MESSAGE, null);
    }
}
