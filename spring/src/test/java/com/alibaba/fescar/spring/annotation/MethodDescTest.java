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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Wu
 * @date 2019/3/8
 */
public class MethodDescTest {

    private GlobalTransactionScanner globalTransactionScanner = new GlobalTransactionScanner(
            "global-trans-scanner-test");

    private static Method METHOD=null;
    private static GlobalTransactional TRANSACTIONAL=null;

    public MethodDescTest() throws NoSuchMethodException {
        METHOD = BusinessImpl.class.getDeclaredMethod("doBiz", new Class[]{String.class});
        TRANSACTIONAL = METHOD.getAnnotation(GlobalTransactional.class);
    }

    @Test(dataProvider = "normalBeanProvider")
    public void testGetTransactionAnnotation(Object bean, String beanName, Object cacheKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc=getMethodDesc();
        assertThat(methodDesc.getTransactionAnnotation()).isEqualTo(TRANSACTIONAL);

    }

    @Test(dataProvider = "normalBeanProvider")
    public void testGetMethod(Object bean, String beanName, Object cacheKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc=getMethodDesc();
        assertThat(methodDesc.getMethod()).isEqualTo(METHOD);
    }

    @Test(dataProvider = "normalBeanProvider")
    public void testSetTransactionAnnotation(Object bean, String beanName, Object cacheKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc=getMethodDesc();
        assertThat(methodDesc.getTransactionAnnotation()).isNotNull();
        methodDesc.setTransactionAnnotation(null);
        assertThat(methodDesc.getTransactionAnnotation()).isNull();
    }

    @Test(dataProvider = "normalBeanProvider")
    public void testSetMethod(Object bean, String beanName, Object cacheKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc=getMethodDesc();
        assertThat(methodDesc.getMethod()).isNotNull();
        methodDesc.setMethod(null);
        assertThat(methodDesc.getMethod()).isNull();
    }



    private MethodDesc getMethodDesc() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //call the private method
        Method m = GlobalTransactionScanner.class.getDeclaredMethod("makeMethodDesc", new Class[]{GlobalTransactional.class, Method.class});
        m.setAccessible(true);
        return (MethodDesc) m.invoke(globalTransactionScanner, TRANSACTIONAL, METHOD);

    }

    @DataProvider
    public static Object[][] normalBeanProvider() {
        Business business = new BusinessImpl();
        String beanName = "business";
        String cacheKey = "business-key";
        return new Object[][]{{business, beanName, cacheKey}};
    }
}
