package io.seata.commonapi.interceptor;

import java.lang.reflect.Method;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public interface InvocationWrapper {

    Method getMethod();

    Object getProxy();

    Object getTarget();

    Object[] getArguments();

    Object proceed();


}
