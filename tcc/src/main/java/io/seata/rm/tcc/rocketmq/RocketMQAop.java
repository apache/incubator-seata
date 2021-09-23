package io.seata.rm.tcc.rocketmq;

import org.apache.rocketmq.client.producer.SendResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import io.seata.core.context.RootContext;

@Aspect
public class RocketMQAop {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQAop.class);

    @Autowired
    TCCRocketMQ tccRocketMQ;

    @Around("execution(* org.apache.rocketmq.client.producer.DefaultMQProducer.send(org.apache.rocketmq.common.message.Message))")
    public SendResult send(ProceedingJoinPoint point) throws Throwable {
        if (RootContext.inGlobalTransaction()) {
            LOGGER.info("DefaultMQProducer send is in Global Transaction, send will be proxy");
            Map<String, Object> map = new HashMap<>();
            map.put("defaultMQProducer", point.getTarget());
            map.put("message", point.getArgs()[0]);
            tccRocketMQ.prepare(null, map);
            Object returnValue = map.get("sendResult");
            return (SendResult) returnValue;
        } else {
            LOGGER.info("DefaultMQProducer send not in Global Transaction, use origin send");
            return (SendResult) point.proceed(point.getArgs());
        }
    }
}
