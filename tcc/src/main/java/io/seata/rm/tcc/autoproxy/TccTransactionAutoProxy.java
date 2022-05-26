package io.seata.rm.tcc.autoproxy;

import io.seata.rm.tcc.interceptor.TCCBeanParserUtils;
import io.seata.rm.tcc.interceptor.TccActionInterceptor;
import io.seata.spring.autoproxy.TransactionAutoProxy;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.context.ApplicationContext;

/**
 * the tcc implements of TransactionAutoProxy
 *
 * @author ruishansun
 */
public class TccTransactionAutoProxy implements TransactionAutoProxy {

    @Override
    public MethodInterceptor isTransactionAutoProxy(Object bean, String beanName, ApplicationContext applicationContext) {
        if (TCCBeanParserUtils.isTccAutoProxy(bean, beanName, applicationContext)) {
            // init tcc fence clean task if enable useTccFence
            TCCBeanParserUtils.initTccFenceCleanTask(TCCBeanParserUtils.getRemotingDesc(beanName), applicationContext);
            //TCC interceptor, proxy bean of sofa:reference/dubbo:reference, and LocalTCC
            return new TccActionInterceptor(TCCBeanParserUtils.getRemotingDesc(beanName));
        }
        return null;
    }
}
