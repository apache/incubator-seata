package io.seata.spring.autoproxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.context.ApplicationContext;

/**
 * The interface Transaction Auto Proxy.
 * if result is not null, then proxied by tcc/saga with SPI.
 *
 * @author ruishansun
 */
public interface TransactionAutoProxy {

    /**
     * if it is transaction auto proxy? (tcc or saga)
     *
     * @param bean               the bean
     * @param beanName           the beanName
     * @param applicationContext the applicationContext
     * @return the MethodInterceptor
     */
    MethodInterceptor isTransactionAutoProxy(Object bean, String beanName, ApplicationContext applicationContext);
}
