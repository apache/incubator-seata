package org.apache.seata.spring.tcc;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler;
import org.apache.seata.spring.annotation.AdapterInvocationWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class TccActionInterceptorHandlerTest {

    protected TccActionInterceptorHandler tccActionInterceptorHandler = new TccActionInterceptorHandler(
            null,
            new HashSet<String>() {{
                add("prepare");
            }}
    );

    /**
     * Test method "parseAnnotation" of TccActionInterceptorHandler
     *
     * @throws Throwable
     */
    @Test
    void testParseAnnotation() throws Throwable {
        // mock MethodInvocation
        NormalTccActionImpl tccAction = new NormalTccActionImpl();
        Method classMethod = NormalTccActionImpl.class.getMethod("prepare", BusinessActionContext.class);
        MethodInvocation mockInvocation = mock(MethodInvocation.class);
        when(mockInvocation.getMethod()).thenReturn(classMethod);
        when(mockInvocation.getArguments()).thenReturn(new Object[]{new BusinessActionContext()});
        when(mockInvocation.proceed()).thenAnswer(invocation -> classMethod.invoke(tccAction, mockInvocation.getArguments()));

        // mock AdapterInvocationWrapper
        AdapterInvocationWrapper invocationWrapper = new AdapterInvocationWrapper(mockInvocation);
        when(invocationWrapper.getTarget()).thenReturn(tccAction);

        // invoke private method "parseAnnotation" of TccActionInterceptorHandler
        Method method = TccActionInterceptorHandler.class.getDeclaredMethod("parseAnnotation", InvocationWrapper.class);
        method.setAccessible(true);
        Object[] results = (Object[]) method.invoke(tccActionInterceptorHandler, invocationWrapper);
        System.out.println(results);

        // test results
        Method interfaceMethod = NormalTccAction.class.getMethod("prepare", BusinessActionContext.class);
        Assertions.assertEquals(interfaceMethod, results[0]);
        Assertions.assertEquals(true, results[1] instanceof TwoPhaseBusinessAction);

    }
}
