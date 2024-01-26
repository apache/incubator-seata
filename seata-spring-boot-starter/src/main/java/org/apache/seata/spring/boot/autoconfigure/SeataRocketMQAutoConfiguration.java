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


import org.apache.seata.integration.rocketmq.SeataMQProducer;
import org.apache.seata.integration.rocketmq.TCCRocketMQ;
import org.apache.seata.integration.rocketmq.TCCRocketMQImpl;
import org.apache.seata.spring.boot.autoconfigure.properties.client.RocketMQProperties;
import org.apache.rocketmq.client.AccessChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * SeataRocketMQAutoConfiguration
 */
@ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
@ConditionalOnExpression("${seata.enabled:true} && ${seata.rocketmq-enabled:false}")
@Configuration(proxyBeanMethods = false)
public class SeataRocketMQAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public TCCRocketMQ tccRocketMQ() {
        return new TCCRocketMQImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public SeataMQProducer seataMqProducer(TCCRocketMQ tccRocketMQ, RocketMQProperties rocketMQProperties) {
        RocketMQProperties.Producer producerConfig = rocketMQProperties.getProducer();
        String nameServer = rocketMQProperties.getNameServer();
        String groupName = producerConfig.getGroup();
        Assert.hasText(nameServer, "[rocketmq.name-server] must not be null");
        Assert.hasText(groupName, "[rocketmq.producer.group] must not be null");

        String accessChannel = rocketMQProperties.getAccessChannel();

        String ak = rocketMQProperties.getProducer().getAccessKey();
        String sk = rocketMQProperties.getProducer().getSecretKey();
        boolean isEnableMsgTrace = rocketMQProperties.getProducer().isEnableMsgTrace();
        String customizedTraceTopic = rocketMQProperties.getProducer().getCustomizedTraceTopic();

        SeataMQProducer producer = SeataMQProducer.create(groupName, ak, sk, isEnableMsgTrace, customizedTraceTopic);

        producer.setNamesrvAddr(nameServer);
        if (StringUtils.hasLength(accessChannel)) {
            producer.setAccessChannel(AccessChannel.valueOf(accessChannel));
        }
        producer.setSendMsgTimeout(producerConfig.getSendMessageTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMessageBodyThreshold());
        producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryNextServer());
        producer.setUseTLS(producerConfig.isTlsEnable());
        producer.setNamespace(producerConfig.getNamespace());
        producer.setInstanceName(producerConfig.getInstanceName());
        producer.setTccRocketMQ(tccRocketMQ);
        tccRocketMQ.setProducer(producer);
        return producer;
    }
}