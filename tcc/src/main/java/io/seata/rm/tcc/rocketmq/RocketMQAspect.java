/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
            LOGGER.info("Generate RocketMQ Producer Proxy");
            tccRocketMQ.setDefaultMQProducer((DefaultMQProducer) bean);
            return new SeataMQProducer((DefaultMQProducer) bean, tccRocketMQ);
        }
        return bean;
    }
}
