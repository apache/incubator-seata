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
package io.seata.spring.boot.autoconfigure;

import io.seata.rm.tcc.rocketmq.RocketMQAspect;
import io.seata.rm.tcc.rocketmq.TCCRocketMQ;
import io.seata.rm.tcc.rocketmq.TCCRocketMQImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@ConditionalOnClass(DefaultMQProducer.class)
@ConditionalOnBean(DefaultMQProducer.class)
@ConditionalOnExpression("${seata.enabled:true} && ${seata.rocketmq-enabled:false")
public class RocketMQAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TCCRocketMQImpl tccRocketMQ(DefaultMQProducer defaultMQProducer) {
        return new TCCRocketMQImpl(defaultMQProducer);
    }

    @Bean
    public RocketMQAspect rocketMQAspect(TCCRocketMQ tccRocketMQ) {
        return new RocketMQAspect(tccRocketMQ);
    }
}