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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * GlobalTransactionScanner Unit Test
 */
public class GlobalTransactionScannerTest {
    private GlobalTransactionScanner globalTransactionScanner = new GlobalTransactionScanner("global-trans-scanner-test");

    @Test(dataProvider = "normalBeanProvider")
    public void testWrapNormalBean(Object bean, String beanName, Object cacheKey) {
        Object result = globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assert.assertNotSame(result, bean);
    }

    @Test
    public void testAfterPropertiesSet() {
        globalTransactionScanner.afterPropertiesSet();
    }

    @DataProvider
    public static Object[][] normalBeanProvider() {
        Business business = new BusinessImpl();
        String beanName = "business";
        String cacheKey = "business-key";
        return new Object[][]{{business, beanName, cacheKey}};
    }
}
