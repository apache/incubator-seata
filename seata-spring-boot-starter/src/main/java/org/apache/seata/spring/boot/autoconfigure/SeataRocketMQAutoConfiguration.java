/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.integration.rocketmq.SeataMQProducerFactory;
import org.apache.seata.integration.rocketmq.TccRocketDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SeataRocketMQAutoConfiguration
 */
@ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
@ConditionalOnExpression("${seata.enabled:true}")
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(SeataAutoConfiguration.class)
public class SeataRocketMQAutoConfiguration {

//    @Bean(name = SeataMQProducerFactory.ROCKET_TCC_NAME)
//    public TCCRocketMQ tccRocketMQ() {
//        return new TCCRocketMQImpl();
//    }
    @Bean
    public TccRocketDefinitionRegistry tccRocketDefinitionRegistry() {
        return new TccRocketDefinitionRegistry();
    }

    @Bean
    public SeataMQProducerFactory seataMQProducerFactory() {
        return new SeataMQProducerFactory();
    }
}