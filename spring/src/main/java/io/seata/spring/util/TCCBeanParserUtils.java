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
package io.seata.spring.util;

import io.seata.common.DefaultValues;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.config.TCCFenceConfig;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.remoting.Protocols;
import io.seata.rm.tcc.remoting.RemotingDesc;
import io.seata.rm.tcc.remoting.RemotingParser;
import io.seata.rm.tcc.remoting.parser.DefaultRemotingParser;
import io.seata.spring.tcc.TccActionInterceptor;
import io.seata.spring.tcc.TccFenceCleaner;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Method;

/**
 * parser TCC bean
 *
 * @author zhangsen
 */
public class TCCBeanParserUtils {

    private static TCCFenceConfig tccFenceConfig = null;

    private TCCBeanParserUtils() {
    }

    /**
     * is auto proxy TCC bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @param applicationContext the application context
     * @return boolean boolean
     */
    public static boolean isTccAutoProxy(Object bean, String beanName, ApplicationContext applicationContext, TccFenceCleaner tccFenceCleaner) {
        boolean isRemotingBean = parserRemotingServiceInfo(bean, beanName);
        //get RemotingBean description
        RemotingDesc remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
        //is remoting bean
        if (isRemotingBean) {
            if (remotingDesc != null && remotingDesc.getProtocol() == Protocols.IN_JVM) {
                //LocalTCC
                return isTccProxyTargetBean(remotingDesc, applicationContext, tccFenceCleaner);
            } else {
                // sofa:reference / dubbo:reference, factory bean
                return false;
            }
        } else {
            if (remotingDesc == null) {
                //check FactoryBean
                if (isRemotingFactoryBean(bean, beanName, applicationContext)) {
                    remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
                    return isTccProxyTargetBean(remotingDesc, applicationContext, tccFenceCleaner);
                } else {
                    return false;
                }
            } else {
                return isTccProxyTargetBean(remotingDesc, applicationContext, tccFenceCleaner);
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
     * is TCC proxy-bean/target-bean: LocalTCC , the proxy bean of sofa:reference/dubbo:reference
     *
     * @param remotingDesc the remoting desc
     * @return boolean boolean
     */
    public static boolean isTccProxyTargetBean(RemotingDesc remotingDesc, ApplicationContext applicationContext, TccFenceCleaner tccFenceCleaner) {
        if (remotingDesc == null) {
            return false;
        }
        //check if it is TCC bean
        boolean isTccClazz = false;
        Class<?> tccInterfaceClazz = remotingDesc.getInterfaceClass();
        Method[] methods = tccInterfaceClazz.getMethods();
        TwoPhaseBusinessAction twoPhaseBusinessAction;
        for (Method method : methods) {
            twoPhaseBusinessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            if (twoPhaseBusinessAction != null) {
                // init tcc fence config if enable useTccFence
                if (twoPhaseBusinessAction.useTCCFence() && applicationContext != null && tccFenceConfig == null) {
                    initTccFenceConfig(applicationContext, tccFenceCleaner);
                }
                isTccClazz = true;
                break;
            }
        }
        if (!isTccClazz) {
            return false;
        }
        short protocols = remotingDesc.getProtocol();
        //LocalTCC
        if (Protocols.IN_JVM == protocols) {
            //in jvm TCC bean , AOP
            return true;
        }
        // sofa:reference /  dubbo:reference, AOP
        return remotingDesc.isReference();
    }

    /**
     * new TccFenceConfig and init tcc fence clean task
     * @param applicationContext the applicationContext
     * @param tccFenceCleaner the tccFenceCleaner
     */
    private static void initTccFenceConfig(ApplicationContext applicationContext, TccFenceCleaner tccFenceCleaner) {
        DataSource dataSource;
        PlatformTransactionManager transactionManager;
        if (applicationContext.containsBean(DefaultValues.TCC_FENCE_DATA_SOURCE_BEAN_NAME)) {
            dataSource = (DataSource) applicationContext.getBean(DefaultValues.TCC_FENCE_DATA_SOURCE_BEAN_NAME);
        } else if (applicationContext.containsBean(DefaultValues.DEFAULT_DATA_SOURCE_BEAN_NAME)) {
            dataSource = (DataSource) applicationContext.getBean(DefaultValues.DEFAULT_DATA_SOURCE_BEAN_NAME);
        } else {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }

        if (applicationContext.containsBean(DefaultValues.TCC_FENCE_TRANSACTION_MANAGER_BEAN_NAME)) {
            transactionManager = (PlatformTransactionManager) applicationContext.getBean(DefaultValues.TCC_FENCE_TRANSACTION_MANAGER_BEAN_NAME);
        } else if (applicationContext.containsBean(DefaultValues.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)) {
            transactionManager = (PlatformTransactionManager) applicationContext.getBean(DefaultValues.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME);
        } else {
            throw new TCCFenceException(FrameworkErrorCode.TransactionManagerNeedInjected);
        }
        // new TCCFenceConfig and init Clean Task
        tccFenceConfig = new TCCFenceConfig(dataSource, transactionManager,
                tccFenceCleaner.getCleanMode(), tccFenceCleaner.getCleanPeriod(), tccFenceCleaner.getLogTableName());
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
     * get the remoting description of TCC bean
     *
     * @param beanName the bean name
     * @return remoting desc
     */
    public static RemotingDesc getRemotingDesc(String beanName) {
        return DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
    }

    /**
     * Create a proxy bean for tcc service
     *
     * @param interfaceClass
     * @param fieldValue
     * @param actionInterceptor
     * @return
     */
    public static <T> T createProxy(Class<T> interfaceClass, Object fieldValue, TccActionInterceptor actionInterceptor) {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(fieldValue);
        factory.setInterfaces(interfaceClass);
        factory.addAdvice(actionInterceptor);

        return (T) factory.getProxy();
    }
}
