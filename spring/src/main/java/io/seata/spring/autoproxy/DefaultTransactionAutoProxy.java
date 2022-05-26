package io.seata.spring.autoproxy;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * the default transaction auto proxy
 *
 * @author ruishansun
 */
public class DefaultTransactionAutoProxy {

    /**
     * all the transaction auto proxy
     */
    protected static List<TransactionAutoProxy> allTransactionAutoProxies = new ArrayList<>();

    private static class SingletonHolder {
        private static final DefaultTransactionAutoProxy INSTANCE = new DefaultTransactionAutoProxy();
    }

    /**
     * Get the default transaction auto proxy
     *
     * @return the default transaction auto proxy
     */
    public static DefaultTransactionAutoProxy get() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Instantiates a new default transaction auto proxy
     */
    protected DefaultTransactionAutoProxy() {
        initTransactionAutoProxy();
    }

    /**
     * init transaction auto proxy
     */
    private void initTransactionAutoProxy() {
        List<TransactionAutoProxy> proxies = EnhancedServiceLoader.loadAll(TransactionAutoProxy.class);
        if (CollectionUtils.isNotEmpty(proxies)) {
            allTransactionAutoProxies.addAll(proxies);
        }
    }

    /**
     * is transaction auto proxy ?
     *
     * @param bean               the bean
     * @param beanName           the beanName
     * @param applicationContext the applicationContext
     * @return the MethodInterceptor or null
     */
    public MethodInterceptor isTransactionAutoProxy(Object bean, String beanName, ApplicationContext applicationContext) {
        for (TransactionAutoProxy proxy : allTransactionAutoProxies) {
            MethodInterceptor methodInterceptor = proxy.isTransactionAutoProxy(bean, beanName, applicationContext);
            if (methodInterceptor != null) {
                return methodInterceptor;
            }
        }
        return null;
    }
}
