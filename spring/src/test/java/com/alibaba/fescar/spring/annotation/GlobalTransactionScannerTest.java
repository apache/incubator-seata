package com.alibaba.fescar.spring.annotation;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;

public class GlobalTransactionScannerTest {
    private GlobalTransactionScanner globalTransactionScanner = new GlobalTransactionScanner("global-trans-scanner-test");

    @Test(dataProvider = "normalBeanProvider")
    public void testWrapNormalBean(Object bean, String beanName, Object cacheKey) {
        globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
    }

    @Test(dataProvider = "proxyBeanProvider")
    public void testWrapProxyBean(Object bean, String beanName, Object cacheKey) {
        globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
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

    @DataProvider
    public static Object[][] proxyBeanProvider() {
        BusinessProxy proxy = new BusinessProxy(new BusinessImpl());
        Business business = (Business) Proxy.newProxyInstance(Business.class.getClassLoader(), new Class[]{Business.class}, proxy);
        String beanName = "business";
        String cacheKey = "business-key";
        return new Object[][]{{business, beanName, cacheKey}};
    }
}
