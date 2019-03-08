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

import static org.assertj.core.api.Assertions.*;

/**
 * @author Wu
 * @date 2019/3/8
 */
public class MethodDescTest {

    private GlobalTransactionScanner globalTransactionScanner = new GlobalTransactionScanner(
            "global-trans-scanner-test");

    @Test(dataProvider = "normalBeanProvider")
    public void testGetTransactionAnnotation(Object bean, String beanName, Object cacheKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = BusinessImpl.class.getDeclaredMethod("doBiz", new Class[]{String.class});
        GlobalTransactional trxAnno = method.getAnnotation(GlobalTransactional.class);

        //call the private method
        Method m = GlobalTransactionScanner.class.getDeclaredMethod("makeMethodDesc", new Class[]{GlobalTransactional.class, Method.class});
        m.setAccessible(true);

        MethodDesc methodDesc = (MethodDesc) m.invoke(globalTransactionScanner, trxAnno, method);

        assertThat(methodDesc.getMethod()).isEqualTo(method);
        assertThat(methodDesc.getTransactionAnnotation()).isEqualTo(trxAnno);

    }

    @DataProvider
    public static Object[][] normalBeanProvider() {
        Business business = new BusinessImpl();
        String beanName = "business";
        String cacheKey = "business-key";
        return new Object[][]{{business, beanName, cacheKey}};
    }
}
