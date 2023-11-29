package io.seata.integration.tx.api.interceptor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author leezongjie
 * @date 2023/11/29
 */
class DefaultInvocationWrapperTest {

    @Test
    void proceed() throws Throwable {
        //given
        MyMockMethodInvocation myMockMethodInvocation = new MyMockMethodInvocation();
        Method method = MyMockMethodInvocation.class.getDeclaredMethod("proceed", int.class);

        //when
        DefaultInvocationWrapper invocationWrapper = new DefaultInvocationWrapper(myMockMethodInvocation, myMockMethodInvocation, method, new Object[]{1});
        //then
        Assertions.assertEquals(1, invocationWrapper.proceed());

        //when
        DefaultInvocationWrapper invocationWrapperThrowException = new DefaultInvocationWrapper(myMockMethodInvocation, myMockMethodInvocation, method, new Object[]{0});
        //then should throw raw exception
        Assertions.assertThrows(ArithmeticException.class, () -> invocationWrapperThrowException.proceed());
    }


    static class MyMockMethodInvocation {
        public Object proceed(int divisor) {
            return 1 / divisor;
        }
    }

}