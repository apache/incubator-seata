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

import com.alibaba.fescar.spring.tcc.LocalTccAction;
import com.alibaba.fescar.spring.tcc.LocalTccActionImpl;
import com.alibaba.fescar.spring.tcc.TccAction;
import com.alibaba.fescar.spring.tcc.TccActionImpl;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * GlobalTransactionScanner Unit Test
 */
public class GlobalTransactionScannerTest {
    /**
     * The Global transaction scanner.
     */
    protected GlobalTransactionScanner globalTransactionScanner = new GlobalTransactionScanner("global-trans-scanner-test");

    /**
     * Test wrap normal bean.
     *
     * @param bean the bean
     * @param beanName the bean name
     * @param cacheKey the cache key
     */
    @Test(dataProvider = "normalBeanProvider")
    public void testWrapNormalBean(Object bean, String beanName, Object cacheKey) {
        Object result = globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assert.assertNotSame(result, bean);
    }

    /**
     * wrap nothing
     *
     * @param bean the bean
     * @param beanName the bean name
     * @param cacheKey the cache key
     */
    @Test(dataProvider = "normalTccBeanProvider")
    public void testWrapNormalTccBean(Object bean, String beanName, Object cacheKey){
        Object result = globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assert.assertSame(result, bean);
    }

    /**
     * wrapped
     *
     * @param bean the bean
     * @param beanName the bean name
     * @param cacheKey the cache key
     */
    @Test(dataProvider = "localTccBeanProvider")
    public void testWrapLocalTccBean(Object bean, String beanName, Object cacheKey){
        TccAction result = (LocalTccAction) globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assert.assertNotSame(result, bean);
    }

    /**
     * Test after properties set.
     */
    @Test
    public void testAfterPropertiesSet() {
        globalTransactionScanner.afterPropertiesSet();
    }

    /**
     * Normal bean provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] normalBeanProvider() {
        Business business = new BusinessImpl();
        String beanName = "business";
        String cacheKey = "business-key";
        return new Object[][]{{business, beanName, cacheKey}};
    }

    /**
     * Normal tcc bean provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] normalTccBeanProvider() {
        TccAction tccAction = new TccActionImpl();
        String beanName = "tccBean";
        String cacheKey = "tccBean-key";
        return new Object[][]{{tccAction, beanName, cacheKey}};
    }

    /**
     * Local tcc bean provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] localTccBeanProvider() {
        LocalTccAction localTccAction = new LocalTccActionImpl();
        String beanName = "lcoalTccBean";
        String cacheKey = "lcoalTccBean-key";
        return new Object[][]{{localTccAction, beanName, cacheKey}};
    }
}
