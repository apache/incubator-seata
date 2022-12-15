package io.seata.commonapi.interceptor.parser;

import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author leezongjie
 * @date 2022/12/15
 */
class GlobalTransactionalInterceptorParserTest {

    @Test
    void parserInterfaceToProxy() throws Exception {

        //given
        BusinessImpl business = new BusinessImpl();

        GlobalTransactionalInterceptorParser globalTransactionalInterceptorParser = new GlobalTransactionalInterceptorParser();

        //when
        ProxyInvocationHandler proxyInvocationHandler = globalTransactionalInterceptorParser.parserInterfaceToProxy(business);

        //then
        Assertions.assertNotNull(proxyInvocationHandler);


    }
}