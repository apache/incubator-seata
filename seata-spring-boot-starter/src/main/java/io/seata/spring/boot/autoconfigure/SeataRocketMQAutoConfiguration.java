package io.seata.spring.boot.autoconfigure;


import io.seata.integration.rocketmq.TCCRocketMQHolder;
import io.seata.integration.rocketmq.TCCRocketMQ;
import io.seata.integration.rocketmq.TCCRocketMQImpl;
import io.seata.integration.tx.api.util.ProxyUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 *
 */
@ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
@ConditionalOnExpression("${seata.enabled:true} && ${seata.rocketmq-enabled:false}")
public class SeataRocketMQAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TCCRocketMQ tccRocketMQ() {
        return new TCCRocketMQImpl();
    }

    @Bean
    public BeanPostProcessor rocketMQBeanPostProcessor(TCCRocketMQ tccRocketMQ) {
        TCCRocketMQHolder.setTCCRocketMQ(tccRocketMQ);
        return new BeanPostProcessor() {

            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                try {
                    Class<?> clazz = Class.forName("org.apache.rocketmq.client.producer.TransactionMQProducer");
                    if (clazz.isInstance(bean)) {
                        Object proxy = ProxyUtil.createProxy(bean, beanName);
                        return proxy;
                    }
                } catch (ClassNotFoundException e) {
                    //todo log?
                    return bean;
                }
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }
        };
    }
}