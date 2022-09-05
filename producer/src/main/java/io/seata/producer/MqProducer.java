package io.seata.producer;


public interface MqProducer {

    void publish(String topic, byte[] sessionBytes);
}
