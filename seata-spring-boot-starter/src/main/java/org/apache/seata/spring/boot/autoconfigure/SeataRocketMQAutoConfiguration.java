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
import org.apache.seata.integration.rocketmq.TCCRocketMQ;
import org.apache.seata.integration.rocketmq.TCCRocketMQImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * SeataRocketMQAutoConfiguration
 */
@ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
@ConditionalOnExpression("${seata.enabled:true} && ${seata.rocketmq-enabled:false}")
@Configuration(proxyBeanMethods = false)
public class SeataRocketMQAutoConfiguration {

    @Autowired
    TCCRocketMQ tccRocketMQ;

    @Bean
    @ConditionalOnMissingBean
    public TCCRocketMQ tccRocketMQ() {
        return new TCCRocketMQImpl();
    }

    @PostConstruct
    public void init() {
        SeataMQProducerFactory.setTccRocketMQ(tccRocketMQ);
    }
}