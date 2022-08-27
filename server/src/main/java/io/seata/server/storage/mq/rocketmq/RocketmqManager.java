package io.seata.server.storage.mq.rocketmq;

import io.seata.server.storage.mq.MqManager;

public class RocketmqManager implements MqManager {
    @Override
    public void publish(String topic, byte[] sessionBytes) {
        throw new UnsupportedOperationException();
    }
}
