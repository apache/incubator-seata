package io.seata.commonapi.util;

import io.seata.commonapi.interceptor.handler.DefaultInvocationHandler;
import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.commonapi.interceptor.parser.DefaultInterfaceParser;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.lang.reflect.Type;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class ProxyUtil {

    public static <T> T createProxy(T target) {
        try {
            ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(target);
            if (proxyInvocationHandler == null) {
                //no need to set
                return target;
            }

            T proxy = (T) new ByteBuddy().subclass(Object.class)
                    .implement(proxyInvocationHandler.getInterfaceToProxy())
                    .intercept(InvocationHandlerAdapter.of(new DefaultInvocationHandler(proxyInvocationHandler, target)))
                    .make()
                    .load(target.getClass().getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
            return proxy;
        } catch (Throwable t) {
            throw new RuntimeException("error occurs when create seata proxy", t);
        }
    }

}
