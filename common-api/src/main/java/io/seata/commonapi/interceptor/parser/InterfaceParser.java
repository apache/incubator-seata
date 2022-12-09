package io.seata.commonapi.interceptor.parser;

import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public interface InterfaceParser {

    ProxyInvocationHandler parserInterfaceToProxy(Object target) throws Exception;


}
