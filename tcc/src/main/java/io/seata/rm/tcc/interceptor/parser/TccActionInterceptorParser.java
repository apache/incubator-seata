package io.seata.rm.tcc.interceptor.parser;

import io.seata.commonapi.interceptor.TxBeanParserUtils;
import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.commonapi.interceptor.parser.InterfaceParser;
import io.seata.rm.tcc.interceptor.TccActionInterceptorHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class TccActionInterceptorParser implements InterfaceParser {

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target) {
        //if it is a tcc remote reference
        if (TxBeanParserUtils.isTxAutoProxy(target, target.toString(), null)) {
            //TODO  获取tcc一阶段接口列表
            Set<String> methodsToProxy = new HashSet<>();
            Class[] interfaceToProxy = target.getClass().getInterfaces();
            ProxyInvocationHandler proxyInvocationHandler = new TccActionInterceptorHandler(interfaceToProxy, methodsToProxy);
            return proxyInvocationHandler;
        }
        return null;
    }
}
