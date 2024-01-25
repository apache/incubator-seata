/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.integration.tx.api.interceptor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
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
