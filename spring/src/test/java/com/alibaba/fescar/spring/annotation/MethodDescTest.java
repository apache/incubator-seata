/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.alibaba.fescar.spring.annotation;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Method desc test.
 *
 * @author Wu
 */
public class MethodDescTest {

    private static final GlobalTransactionScanner GLOBAL_TRANSACTION_SCANNER = new GlobalTransactionScanner("global-trans-scanner-test");
    private static Method method = null;
    private static GlobalTransactional transactional = null;

    /**
     * Instantiates a new Method desc test.
     *
     * @throws NoSuchMethodException the no such method exception
     */
    public MethodDescTest() throws NoSuchMethodException {
        method = MockBusiness.class.getDeclaredMethod("doBiz", String.class);
        transactional = method.getAnnotation(GlobalTransactional.class);
    }

    /**
     * Test get transaction annotation.
     *
     * @throws NoSuchMethodException the no such method exception
     * @throws InvocationTargetException the invocation target exception
     * @throws IllegalAccessException the illegal access exception
     */
    @Test
    public void testGetTransactionAnnotation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getTransactionAnnotation()).isEqualTo(transactional);

    }

    /**
     * Test get method.
     *
     * @throws NoSuchMethodException the no such method exception
     * @throws InvocationTargetException the invocation target exception
     * @throws IllegalAccessException the illegal access exception
     */
    @Test
    public void testGetMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getMethod()).isEqualTo(method);
    }

    /**
     * Test set transaction annotation.
     *
     * @throws NoSuchMethodException the no such method exception
     * @throws InvocationTargetException the invocation target exception
     * @throws IllegalAccessException the illegal access exception
     */
    @Test
    public void testSetTransactionAnnotation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getTransactionAnnotation()).isNotNull();
        methodDesc.setTransactionAnnotation(null);
        assertThat(methodDesc.getTransactionAnnotation()).isNull();
    }

    /**
     * Test set method.
     *
     * @throws NoSuchMethodException the no such method exception
     * @throws InvocationTargetException the invocation target exception
     * @throws IllegalAccessException the illegal access exception
     */
    @Test
    public void testSetMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getMethod()).isNotNull();
        methodDesc.setMethod(null);
        assertThat(methodDesc.getMethod()).isNull();
    }


    private MethodDesc getMethodDesc() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //call the private method
        Method m = GlobalTransactionScanner.class.getDeclaredMethod("makeMethodDesc", GlobalTransactional.class, Method.class);
        m.setAccessible(true);
        return (MethodDesc) m.invoke(GLOBAL_TRANSACTION_SCANNER, transactional, method);

    }

    /**
     * the type mock business
     */
    private static class MockBusiness {
        /**
         * Do biz string.
         *
         * @param msg the msg
         * @return the string
         */
        @GlobalTransactional(timeoutMills = 300000, name = "busi-doBiz")
        public String doBiz(String msg) {
            return "hello " + msg;
        }
    }

}
