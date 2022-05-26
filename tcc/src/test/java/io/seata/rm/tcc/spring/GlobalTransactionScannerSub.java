package io.seata.rm.tcc.spring;

import io.seata.spring.annotation.GlobalTransactionScanner;

/**
 * the subclass of GlobalTransactionScanner for test public method wrapIfNecessary
 */
public class GlobalTransactionScannerSub extends GlobalTransactionScanner {


    public GlobalTransactionScannerSub(String txServiceGroup) {
        super(txServiceGroup);
    }

    public Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        return super.wrapIfNecessary(bean, beanName, cacheKey);
    }
}
