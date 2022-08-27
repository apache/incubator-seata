package io.seata.server.storage.mq.rocketmq;

import io.seata.server.storage.mq.MqProducer;

public class RocketmqProducer implements MqProducer {
    @Override
    public void publish(String topic, byte[] sessionBytes) {
        throw new UnsupportedOperationException();
    }
}
