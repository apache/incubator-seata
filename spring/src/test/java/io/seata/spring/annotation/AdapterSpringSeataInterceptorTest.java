package io.seata.spring.annotation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import io.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.spring.tcc.NormalTccAction;
import io.seata.spring.tcc.NormalTccActionImpl;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author leezongjie
 * @date 2023/11/29
 */
class AdapterSpringSeataInterceptorTest {

    @Test
    void should_throw_raw_exception_when_call_prepareWithException() throws Throwable {
        //given
        NormalTccActionImpl normalTccAction = new NormalTccActionImpl();
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(normalTccAction, "proxyTccAction");
        AdapterSpringSeataInterceptor adapterSpringSeataInterceptor = new AdapterSpringSeataInterceptor(proxyInvocationHandler);
        MyMockMethodInvocation myMockMethodInvocation = new MyMockMethodInvocation(NormalTccAction.class.getMethod("prepareWithException", BusinessActionContext.class), () -> normalTccAction.prepareWithException(null));

        //when then
        Assertions.assertThrows(IllegalArgumentException.class, () -> adapterSpringSeataInterceptor.invoke(myMockMethodInvocation));
    }

    @Test
    void should_success_when_call_prepare_with_ProxyInvocationHandler() throws Throwable {
        //given
        NormalTccActionImpl normalTccAction = new NormalTccActionImpl();
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(normalTccAction, "proxyTccAction");
        AdapterSpringSeataInterceptor adapterSpringSeataInterceptor = new AdapterSpringSeataInterceptor(proxyInvocationHandler);
        MyMockMethodInvocation myMockMethodInvocation = new MyMockMethodInvocation(NormalTccAction.class.getMethod("prepare", BusinessActionContext.class), () -> normalTccAction.prepare(null));

        //when then
        Assertions.assertTrue((Boolean) adapterSpringSeataInterceptor.invoke(myMockMethodInvocation));
    }

    static class MyMockMethodInvocation implements MethodInvocation {

        private Callable callable;
        private Method method;

        public MyMockMethodInvocation(Method method, Callable callable) {
            this.method = method;
            this.callable = callable;
        }

        @Nullable
        @Override
        public Object proceed() throws Throwable {
            return callable.call();
        }

        @Nonnull
        @Override
        public Method getMethod() {
            return method;
        }

        @Nonnull
        @Override
        public Object[] getArguments() {
            return new Object[0];
        }

        @Nullable
        @Override
        public Object getThis() {
            return null;
        }

        @Nonnull
        @Override
        public AccessibleObject getStaticPart() {
            return null;
        }
    }
}