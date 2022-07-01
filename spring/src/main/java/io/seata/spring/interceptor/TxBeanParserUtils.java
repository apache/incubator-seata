/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.spring.interceptor;

import io.seata.common.DefaultValues;
import io.seata.spring.autoproxy.DefaultTransactionAutoProxy;
import io.seata.spring.fence.config.CommonFenceConfig;
import io.seata.spring.remoting.Protocols;
import io.seata.spring.remoting.RemotingDesc;
import io.seata.spring.remoting.RemotingParser;
import io.seata.spring.remoting.parser.DefaultRemotingParser;
import io.seata.spring.util.SpringProxyUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;

/**
 * parser transaction bean
 *
 * @author zhangsen
 */
public class TxBeanParserUtils {

    private TxBeanParserUtils() {
    }

    /**
     * is auto proxy transaction bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @param applicationContext the application context
     * @return boolean boolean
     */
    public static boolean isTxAutoProxy(Object bean, String beanName, ApplicationContext applicationContext) {
        boolean isRemotingBean = parserRemotingServiceInfo(bean, beanName);
        //get RemotingBean description
        RemotingDesc remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
        //is remoting bean
        if (isRemotingBean) {
            if (remotingDesc != null && remotingDesc.getProtocol() == Protocols.IN_JVM) {
                //LocalService
                return DefaultTransactionAutoProxy.get().isTransactionAutoProxy(beanName, remotingDesc);
            } else {
                // sofa:reference / dubbo:reference, factory bean
                return false;
            }
        } else {
            if (remotingDesc == null) {
                //check FactoryBean
                if (isRemotingFactoryBean(bean, beanName, applicationContext)) {
                    remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
                    return DefaultTransactionAutoProxy.get().isTransactionAutoProxy(beanName, remotingDesc);
                } else {
                    return false;
                }
            } else {
                return DefaultTransactionAutoProxy.get().isTransactionAutoProxy(beanName, remotingDesc);
            }
        }
    }

    /**
     * if it is proxy bean, check if the FactoryBean is Remoting bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @param applicationContext the application context
     * @return boolean boolean
     */
    protected static boolean isRemotingFactoryBean(Object bean, String beanName,
                                                   ApplicationContext applicationContext) {
        if (!SpringProxyUtils.isProxy(bean)) {
            return false;
        }
        //the FactoryBean of proxy bean
        String factoryBeanName = "&" + beanName;
        Object factoryBean = null;
        if (applicationContext != null && applicationContext.containsBean(factoryBeanName)) {
            factoryBean = applicationContext.getBean(factoryBeanName);
        }
        //not factory bean, needn't proxy
        if (factoryBean == null) {
            return false;
        }
        //get FactoryBean info
        return parserRemotingServiceInfo(factoryBean, beanName);
    }

    /**
     * init common fence clean task if enable useCommonFence
     *
     * @param remotingDesc the remoting desc
     * @param applicationContext applicationContext
     */
    public static void initCommonFenceCleanTask(RemotingDesc remotingDesc, ApplicationContext applicationContext, boolean useCommonFence) {
        if (remotingDesc == null) {
            return;
        }
        if (applicationContext != null && applicationContext.containsBean(DefaultValues.COMMON_FENCE_BEAN_NAME)) {
            CommonFenceConfig commonFenceConfig = (CommonFenceConfig) applicationContext.getBean(DefaultValues.COMMON_FENCE_BEAN_NAME);
            if (commonFenceConfig == null || commonFenceConfig.getInitialized().get()) {
                return;
            }

            if (useCommonFence && commonFenceConfig.getInitialized().compareAndSet(false, true)) {
                // init common fence clean task if enable useCommonFence
                commonFenceConfig.initCleanTask();
            }
        }
    }

    /**
     * get remoting bean info: sofa:service, sofa:reference, dubbo:reference, dubbo:service
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return if sofa:service, sofa:reference, dubbo:reference, dubbo:service return true, else return false
     */
    protected static boolean parserRemotingServiceInfo(Object bean, String beanName) {
        RemotingParser remotingParser = DefaultRemotingParser.get().isRemoting(bean, beanName);
        if (remotingParser != null) {
            return DefaultRemotingParser.get().parserRemotingServiceInfo(bean, beanName, remotingParser) != null;
        }
        return false;
    }

    /**
     * get the remoting description of Tx bean
     *
     * @param beanName the bean name
     * @return remoting desc
     */
    public static RemotingDesc getRemotingDesc(String beanName) {
        return DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
    }

    /**
     * Create a proxy bean for transaction service
     *
     * @param interfaceClass
     * @param fieldValue
     * @param actionInterceptor
     * @return
     */
    public static <T> T createProxy(Class<T> interfaceClass, Object fieldValue, MethodInterceptor actionInterceptor) {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(fieldValue);
        factory.setInterfaces(interfaceClass);
        factory.addAdvice(actionInterceptor);

        return (T) factory.getProxy();
    }
}
