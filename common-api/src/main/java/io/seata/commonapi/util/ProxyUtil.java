package io.seata.commonapi.util;

import io.seata.commonapi.interceptor.handler.DefaultInvocationHandler;
import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.commonapi.interceptor.parser.DefaultInterfaceParser;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class ProxyUtil {

    private static final Map<Object, Object> PROXYED_SET = new HashMap<>();

    public static <T> T createProxy(T target) {
        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.containsKey(target)) {
                    return (T) PROXYED_SET.get(target);
                }
                ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(target);
                if (proxyInvocationHandler == null) {
                    return target;
                }
                DynamicType.Builder.MethodDefinition.ImplementationDefinition<?> implementationDefinition;
                if (proxyInvocationHandler.interfaceProxyMode()) {
                    implementationDefinition = new ByteBuddy().subclass(Object.class)
                            .implement(proxyInvocationHandler.getInterfaceToProxy());
                } else {
                    implementationDefinition = new ByteBuddy()
                            .subclass(target.getClass())
                            .method(isDeclaredBy(target.getClass()));
                }
                T proxy = (T) implementationDefinition.intercept(InvocationHandlerAdapter.of(new DefaultInvocationHandler(proxyInvocationHandler, target)))
                        .make()
                        .load(target.getClass().getClassLoader())
                        .getLoaded()
                        .getDeclaredConstructor()
                        .newInstance();
                PROXYED_SET.put(target, proxy);
                return proxy;
            }
        } catch (Throwable t) {
            throw new RuntimeException("error occurs when create seata proxy", t);
        }
    }

}
