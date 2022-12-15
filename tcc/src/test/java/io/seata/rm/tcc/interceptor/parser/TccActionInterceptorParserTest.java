package io.seata.rm.tcc.interceptor.parser;

import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.rm.tcc.NormalTccActionImpl;
import io.seata.rm.tcc.TccAction;
import io.seata.rm.tcc.TccActionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author leezongjie
 * @date 2022/12/8
 */
class TccActionInterceptorParserTest {

    @Test
    void parserInterfaceToProxy() {

        //given
        TccActionInterceptorParser tccActionInterceptorParser = new TccActionInterceptorParser();
        NormalTccActionImpl tccAction = new NormalTccActionImpl();

        //when
        ProxyInvocationHandler proxyInvocationHandler = tccActionInterceptorParser.parserInterfaceToProxy(tccAction);

        //then
        Assertions.assertNotNull(proxyInvocationHandler);

    }
}