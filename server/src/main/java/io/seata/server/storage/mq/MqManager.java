package io.seata.server.storage.mq;


public interface MqManager {

    void publish(String topic, byte[] sessionBytes) ;
}
