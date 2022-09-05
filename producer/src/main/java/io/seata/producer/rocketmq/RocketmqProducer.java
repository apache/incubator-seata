package io.seata.producer.rocketmq;

import io.seata.producer.MqProducer;

public class RocketmqProducer implements MqProducer {

    @Override
    public void publish(String topic, byte[] key, byte[] value) {
        throw new UnsupportedOperationException();
    }
}
