package io.seata.rm.tcc.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class RocketMQAspect implements BeanPostProcessor {
    public static Logger LOGGER = LoggerFactory.getLogger(RocketMQAspect.class);

    private final TCCRocketMQ tccRocketMQ;

    public RocketMQAspect(TCCRocketMQ tccRocketMQ) {
        this.tccRocketMQ = tccRocketMQ;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DefaultMQProducer) {
            LOGGER.warn("生成代理类");
            tccRocketMQ.setDefaultMQProducer((DefaultMQProducer) bean);
            return new SeataMQProducer((DefaultMQProducer) bean, tccRocketMQ);
        }
        return bean;
    }
}
