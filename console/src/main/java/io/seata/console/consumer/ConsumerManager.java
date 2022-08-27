package io.seata.console.consumer;

import org.springframework.stereotype.Component;

import java.util.ServiceLoader;

@Component
public class ConsumerManager implements ConsumerInterface {

    public ConsumerManager() {
        consume();
    }

    @Override
    public void consume() {
        ServiceLoader<ConsumerInterface> consumers = ServiceLoader.load(ConsumerInterface.class);
        for (ConsumerInterface consumer : consumers) {
            consumer.consume();
        }
    }
}
