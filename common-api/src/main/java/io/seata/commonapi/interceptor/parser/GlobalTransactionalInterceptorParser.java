package io.seata.commonapi.interceptor.parser;

import io.seata.common.util.CollectionUtils;
import io.seata.commonapi.interceptor.handler.GlobalTransactionalInterceptorHandler;
import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.DefaultFailureHandlerImpl;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class GlobalTransactionalInterceptorParser implements InterfaceParser {

    private Set<String> methodsToProxy = new HashSet<>();

    /**
     * @param target
     * @return
     * @throws Exception
     * @see GlobalTransactional // TM annotation
     * <p>
     * GlobalLock:
     * @see GlobalLock // GlobalLock annotation
     */
    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target) throws Exception {
        Class<?> serviceInterface = DefaultTargetClassParser.get().findTargetClass(target);
        Class<?>[] interfacesIfJdk = DefaultTargetClassParser.get().findInterfaces(target);

        if (existsAnnotation(new Class[]{serviceInterface}) || existsAnnotation(interfacesIfJdk)) {
            Class[] interfaceToProxy = target.getClass().getInterfaces();
            ProxyInvocationHandler proxyInvocationHandler = new GlobalTransactionalInterceptorHandler(new DefaultFailureHandlerImpl(), interfaceToProxy, null);
            return proxyInvocationHandler;
        }

        return null;
    }

    private boolean existsAnnotation(Class<?>[] classes) {
        boolean result = false;
        if (CollectionUtils.isNotEmpty(classes)) {
            for (Class<?> clazz : classes) {
                if (clazz == null) {
                    continue;
                }
                GlobalTransactional trxAnno = clazz.getAnnotation(GlobalTransactional.class);
                if (trxAnno != null) {
                    return true;
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    trxAnno = method.getAnnotation(GlobalTransactional.class);
                    if (trxAnno != null) {
                        methodsToProxy.add(method.getName());
                        result = true;
                    }

                    GlobalLock lockAnno = method.getAnnotation(GlobalLock.class);
                    if (lockAnno != null) {
                        methodsToProxy.add(method.getName());
                        result = true;
                    }
                }
            }
        }
        return result;
    }
}
