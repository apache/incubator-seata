package io.seata.commonapi.interceptor.handler;

import io.seata.commonapi.interceptor.InvocationWrapper;

import java.util.Set;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public interface ProxyInvocationHandler {

    Class[] getInterfaceToProxy();

    Set<String> getMethodsToProxy();

    boolean interfaceProxyMode();

    Object invoke(InvocationWrapper invocation) throws Throwable;

}
