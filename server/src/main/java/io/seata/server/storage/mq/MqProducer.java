package io.seata.server.storage.mq;


public interface MqProducer {

    void publish(String topic, byte[] sessionBytes) ;
}
