package io.seata.rm.tcc.rocketmq;

import org.apache.rocketmq.client.Validators;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;

import java.lang.reflect.Field;

public class MQUtils {
    private static final String PRODUCER_IMPL = "defaultMQProducerImpl";

    public static SendResult halfSend(DefaultMQProducer defaultMQproducer, Message msg) {
        // ignore DelayTimeLevel parameter
        if (msg.getDelayTimeLevel() != 0) {
            MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_DELAY_TIME_LEVEL);
        }

        try {
            Validators.checkMessage(msg, defaultMQproducer);
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }

        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_PRODUCER_GROUP, defaultMQproducer.getProducerGroup());
        DefaultMQProducerImpl defaultMQProducerImpl = (DefaultMQProducerImpl) getFieldByReflect(defaultMQproducer, PRODUCER_IMPL);
        SendResult sendResult = null;
        try {
            sendResult = defaultMQProducerImpl.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Send message Exception", e);
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

    public static void confirm(DefaultMQProducer defaultMQProducer, Message msg, SendResult sendResult) {
        DefaultMQProducerImpl defaultMQProducerImpl = (DefaultMQProducerImpl) getFieldByReflect(defaultMQProducer, PRODUCER_IMPL);
        try {
            defaultMQProducerImpl.endTransaction(msg, sendResult, LocalTransactionState.COMMIT_MESSAGE, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void cancel(DefaultMQProducer defaultMQProducer, Message msg, SendResult sendResult) {
        DefaultMQProducerImpl defaultMQProducerImpl = (DefaultMQProducerImpl) getFieldByReflect(defaultMQProducer, PRODUCER_IMPL);
        try {
            defaultMQProducerImpl.endTransaction(msg, sendResult, LocalTransactionState.ROLLBACK_MESSAGE, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getFieldByReflect(Object instance, String fieldName) {
        Object value = null;
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            value = field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }
}
