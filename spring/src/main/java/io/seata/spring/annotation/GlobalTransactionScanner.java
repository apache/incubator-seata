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
package io.seata.spring.annotation;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationCache;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.rpc.netty.RmNettyRemotingClient;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.rm.RMClient;
import io.seata.spring.tcc.TccActionInterceptor;
import io.seata.spring.util.SpringProxyUtils;
import io.seata.spring.util.TCCBeanParserUtils;
import io.seata.tm.TMClient;
import io.seata.tm.api.FailureHandler;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;


import static io.seata.core.constants.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;

/**
 * The type Global transaction scanner.
 *
 * @author slievrly
 */
public class GlobalTransactionScanner extends AbstractAutoProxyCreator
    implements InitializingBean, ApplicationContextAware,
    DisposableBean {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionScanner.class);

    private static final int AT_MODE = 1;
    private static final int MT_MODE = 2;

    private static final int ORDER_NUM = 1024;
    private static final int DEFAULT_MODE = AT_MODE + MT_MODE;

    private static final Set<String> PROXYED_SET = new HashSet<>();

    private MethodInterceptor interceptor;
    private MethodInterceptor globalTransactionalInterceptor;

    private final String applicationId;
    private final String txServiceGroup;
    private final int mode;
    private final boolean disableGlobalTransaction = ConfigurationFactory.getInstance().getBoolean(
        ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);

    private final FailureHandler failureHandlerHook;

    private ApplicationContext applicationContext;

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param txServiceGroup the tx service group
     */
    public GlobalTransactionScanner(String txServiceGroup) {
        this(txServiceGroup, txServiceGroup, DEFAULT_MODE);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param txServiceGroup the tx service group
     * @param mode           the mode
     */
    public GlobalTransactionScanner(String txServiceGroup, int mode) {
        this(txServiceGroup, txServiceGroup, mode);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the default server group
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup) {
        this(applicationId, txServiceGroup, DEFAULT_MODE);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the tx service group
     * @param mode           the mode
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode) {
        this(applicationId, txServiceGroup, mode, null);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, FailureHandler failureHandlerHook) {
        this(applicationId, txServiceGroup, DEFAULT_MODE, failureHandlerHook);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param mode               the mode
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode,
                                    FailureHandler failureHandlerHook) {
        setOrder(ORDER_NUM);
        setProxyTargetClass(true);
        this.applicationId = applicationId;
        this.txServiceGroup = txServiceGroup;
        this.mode = mode;
        this.failureHandlerHook = failureHandlerHook;
    }

    @Override
    public void destroy() {
        ShutdownHook.getInstance().destroyAll();
    }

    private void initClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (StringUtils.isNullOrEmpty(applicationId) || StringUtils.isNullOrEmpty(txServiceGroup)) {
            throw new IllegalArgumentException(String.format("applicationId: %s, txServiceGroup: %s", applicationId, txServiceGroup));
        }
        //init TM
        TMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Transaction Manager Client is initialized. applicationId[{}] txServiceGroup[{}]", applicationId, txServiceGroup);
        }
        //init RM
        RMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Resource Manager is initialized. applicationId[{}] txServiceGroup[{}]", applicationId, txServiceGroup);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Global Transaction Clients are initialized. ");
        }
        registerSpringShutdownHook();

    }

    private void registerSpringShutdownHook() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).registerShutdownHook();
            ShutdownHook.removeRuntimeShutdownHook();
        }
        ShutdownHook.getInstance().addDisposable(TmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
        ShutdownHook.getInstance().addDisposable(RmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (disableGlobalTransaction) {
            return bean;
        }
        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.contains(beanName)) {
                    return bean;
                }
                interceptor = null;
                //check TCC proxy
                if (TCCBeanParserUtils.isTccAutoProxy(bean, beanName, applicationContext)) {
                    //TCC interceptor, proxy bean of sofa:reference/dubbo:reference, and LocalTCC
                    interceptor = new TccActionInterceptor(TCCBeanParserUtils.getRemotingDesc(beanName));
                } else {
                    Class<?> serviceInterface = SpringProxyUtils.findTargetClass(bean);
                    Class<?>[] interfacesIfJdk = SpringProxyUtils.findInterfaces(bean);

                    if (!existsAnnotation(new Class[]{serviceInterface})
                        && !existsAnnotation(interfacesIfJdk)) {
                        return bean;
                    }

                    if (interceptor == null) {
                        if (globalTransactionalInterceptor == null) {
                            globalTransactionalInterceptor = new GlobalTransactionalInterceptor(failureHandlerHook);
                            ConfigurationCache.addConfigListener(
                                ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                                (ConfigurationChangeListener)globalTransactionalInterceptor);
                        }
                        interceptor = globalTransactionalInterceptor;
                    }
                }

                LOGGER.info("Bean[{}] with name [{}] would use interceptor [{}]", bean.getClass().getName(), beanName, interceptor.getClass().getName());
                if (!AopUtils.isAopProxy(bean)) {
                    bean = super.wrapIfNecessary(bean, beanName, cacheKey);
                } else {
                    AdvisedSupport advised = SpringProxyUtils.getAdvisedSupport(bean);
                    Advisor[] advisor = buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(null, null, null));
                    for (Advisor avr : advisor) {
                        advised.addAdvisor(0, avr);
                    }
                }
                PROXYED_SET.add(beanName);
                return bean;
            }
        } catch (Exception exx) {
            throw new RuntimeException(exx);
        }
    }

    private boolean existsAnnotation(Class<?>[] classes) {
        if (CollectionUtils.isNotEmpty(classes)) {
            for (Class<?> clazz : classes) {
                if (clazz == null) {
                    continue;
                }
                GlobalTransactional trxAnno = clazz.getAnnotation(GlobalTransactional.class);
                if (trxAnno != null) {
                    return true;
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    trxAnno = method.getAnnotation(GlobalTransactional.class);
                    if (trxAnno != null) {
                        return true;
                    }

                    GlobalLock lockAnno = method.getAnnotation(GlobalLock.class);
                    if (lockAnno != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private MethodDesc makeMethodDesc(GlobalTransactional anno, Method method) {
        return new MethodDesc(anno, method);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class beanClass, String beanName, TargetSource customTargetSource)
        throws BeansException {
        return new Object[]{interceptor};
    }

    @Override
    public void afterPropertiesSet() {
        if (disableGlobalTransaction) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Global transaction is disabled.");
            }
            return;
        }
        initClient();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.setBeanFactory(applicationContext);
    }
}
