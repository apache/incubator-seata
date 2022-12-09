package io.seata.commonapi.interceptor.handler;

import io.seata.commonapi.interceptor.InvocationWrapper;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public interface ProxyInvocationHandler {

    Class[] getInterfaceToProxy();

    Object invoke(InvocationWrapper invocation) throws Throwable;

}
