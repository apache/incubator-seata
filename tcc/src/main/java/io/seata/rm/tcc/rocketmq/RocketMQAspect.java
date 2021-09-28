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

import io.seata.core.context.RootContext;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class RocketMQAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQAspect.class);

    private final TCCRocketMQ tccRocketMQ;

    public RocketMQAspect(TCCRocketMQ tccRocketMQ) {
        this.tccRocketMQ = tccRocketMQ;
    }

    @Around("execution(* org.apache.rocketmq.client.producer.DefaultMQProducer.send(org.apache.rocketmq.common.message.Message))")
    public SendResult send(ProceedingJoinPoint point) throws Throwable {
        if (RootContext.inGlobalTransaction()) {
            LOGGER.info("DefaultMQProducer send is in Global Transaction, send() will be proxy");
            Message message = (Message) point.getArgs()[0];
            return tccRocketMQ.prepare(null, message);
        } else {
            return (SendResult) point.proceed();
        }
    }
}
