package io.seata.integration.rocketmq;

import io.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import io.seata.integration.tx.api.interceptor.parser.InterfaceParser;
import org.apache.rocketmq.client.producer.TransactionMQProducer;

import java.util.HashSet;
import java.util.Set;

/**
 * ?
 *
 * @author minghua.xie
 * @date 2023/12/28
 **/
public class RocketMQInterceptorParser implements InterfaceParser {


    private final Set<String> methodsToProxy = new HashSet<>();


    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target, String objectName) throws Exception {
        if (target instanceof TransactionMQProducer) {
            TCCRocketMQ tccRocket;
            ProxyInvocationHandler proxyInvocationHandler = new RocketMQInterceptorHandler((TransactionMQProducer) target);
        }

        return null;
    }


}
