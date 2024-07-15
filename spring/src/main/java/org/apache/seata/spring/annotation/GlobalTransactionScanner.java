/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.CachedConfigurationChangeListener;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.rpc.ShutdownHook;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.integration.tx.api.annotation.AspectTransactional;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptor;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import org.apache.seata.integration.tx.api.interceptor.parser.IfNeedEnhanceBean;
import org.apache.seata.integration.tx.api.interceptor.parser.NeedEnhanceEnum;
import org.apache.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import org.apache.seata.rm.RMClient;
import org.apache.seata.spring.annotation.scannercheckers.PackageScannerChecker;
import org.apache.seata.spring.remoting.parser.RemotingFactoryBeanParser;
import org.apache.seata.spring.util.OrderUtil;
import org.apache.seata.spring.util.SpringProxyUtils;
import org.apache.seata.tm.TMClient;
import org.apache.seata.tm.api.FailureHandler;
import org.apache.seata.tm.api.FailureHandlerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;

import static org.apache.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP_OLD;

/**
 * The type Global transaction scanner.
 *
 */
public class GlobalTransactionScanner extends AbstractAutoProxyCreator
        implements CachedConfigurationChangeListener, InitializingBean, ApplicationContextAware, DisposableBean {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionScanner.class);

    private static final int AT_MODE = 1;
    private static final int MT_MODE = 2;

    private static final int ORDER_NUM = 1024;
    private static final int DEFAULT_MODE = AT_MODE + MT_MODE;

    private static final String SPRING_TRANSACTION_INTERCEPTOR_CLASS_NAME = "org.springframework.transaction.interceptor.TransactionInterceptor";

    private static final Set<String> PROXYED_SET = new HashSet<>();
    private static final Set<String> EXCLUDE_BEAN_NAME_SET = new HashSet<>();
    private static final Set<ScannerChecker> SCANNER_CHECKER_SET = new LinkedHashSet<>();

    private static ConfigurableListableBeanFactory beanFactory;

    private MethodInterceptor interceptor;

    private final String applicationId;
    private final String txServiceGroup;
    private static String accessKey;
    private static String secretKey;
    private volatile boolean disableGlobalTransaction = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final FailureHandler failureHandlerHook;

    private ApplicationContext applicationContext;

    private static final Set<String> NEED_ENHANCE_BEAN_NAME_SET = new HashSet<>();


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
        this(applicationId, txServiceGroup, mode, false, null);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, FailureHandler failureHandlerHook) {
        this(applicationId, txServiceGroup, DEFAULT_MODE, false, failureHandlerHook);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param exposeProxy        the exposeProxy
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, boolean exposeProxy,
                                    FailureHandler failureHandlerHook) {
        this(applicationId, txServiceGroup, DEFAULT_MODE, exposeProxy, failureHandlerHook);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param mode               the mode
     * @param exposeProxy        the exposeProxy
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode, boolean exposeProxy,
                                    FailureHandler failureHandlerHook) {
        setOrder(ORDER_NUM);
        setProxyTargetClass(true);
        setExposeProxy(exposeProxy);
        this.applicationId = applicationId;
        this.txServiceGroup = txServiceGroup;
        this.failureHandlerHook = failureHandlerHook;
        FailureHandlerHolder.setFailureHandler(this.failureHandlerHook);
    }

    /**
     * Sets access key.
     *
     * @param accessKey the access key
     */
    public static void setAccessKey(String accessKey) {
        GlobalTransactionScanner.accessKey = accessKey;
    }

    /**
     * Sets secret key.
     *
     * @param secretKey the secret key
     */
    public static void setSecretKey(String secretKey) {
        GlobalTransactionScanner.secretKey = secretKey;
    }

    @Override
    public void destroy() {
        ShutdownHook.getInstance().destroyAll();
    }

    protected void initClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (DEFAULT_TX_GROUP_OLD.equals(txServiceGroup)) {
            LOGGER.warn("the default value of seata.tx-service-group: {} has already changed to {} since Seata 1.5, " +
                            "please change your default configuration as soon as possible " +
                            "and we don't recommend you to use default tx-service-group's value provided by seata",
                    DEFAULT_TX_GROUP_OLD, DEFAULT_TX_GROUP);
        }
        if (StringUtils.isNullOrEmpty(applicationId) || StringUtils.isNullOrEmpty(txServiceGroup)) {
            throw new IllegalArgumentException(String.format("applicationId: %s, txServiceGroup: %s", applicationId, txServiceGroup));
        }
        //init TM
        TMClient.init(applicationId, txServiceGroup, accessKey, secretKey);
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

    protected void registerSpringShutdownHook() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).registerShutdownHook();
            ShutdownHook.removeRuntimeShutdownHook();
        }
        ShutdownHook.getInstance().addDisposable(TmNettyRemotingClient.getInstance(applicationId, txServiceGroup, accessKey, secretKey));
        ShutdownHook.getInstance().addDisposable(RmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
    }

    /**
     * The following will be scanned, and added corresponding interceptor:
     * <p>
     * TM:
     *
     * @see org.apache.seata.spring.annotation.GlobalTransactional // TM annotation
     * Corresponding interceptor:org.apache.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler
     * @see GlobalTransactionalInterceptorHandler#handleGlobalTransaction(InvocationWrapper, AspectTransactional) // TM handler
     * <p>
     * GlobalLock:
     * @see org.apache.seata.spring.annotation.GlobalLock // GlobalLock annotation
     * Corresponding interceptor:
     * @see GlobalTransactionalInterceptorHandler#handleGlobalLock(InvocationWrapper, GlobalLock)  // GlobalLock handler
     * <p>
     * TCC mode:
     * @see org.apache.seata.rm.tcc.api.LocalTCC // TCC annotation on interface
     * @see org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction // TCC annotation on try method
     * @see org.apache.seata.integration.tx.api.remoting.RemotingParser // Remote TCC service parser
     * Corresponding interceptor:
     * @see org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler // the interceptor of TCC mode
     */
    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // do checkers
        if (!doCheckers(bean, beanName)) {
            return bean;
        }

        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.contains(beanName)) {
                    return bean;
                }
                if (!NEED_ENHANCE_BEAN_NAME_SET.contains(beanName)) {
                    return bean;
                }
                interceptor = null;
                ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(bean, beanName);
                if (proxyInvocationHandler == null) {
                    return bean;
                }

                interceptor = new AdapterSpringSeataInterceptor(proxyInvocationHandler);

                LOGGER.info("Bean [{}] with name [{}] would use interceptor [{}]", bean.getClass().getName(), beanName, interceptor.toString());
                if (!AopUtils.isAopProxy(bean)) {
                    bean = super.wrapIfNecessary(bean, beanName, cacheKey);
                } else {
                    AdvisedSupport advised = SpringProxyUtils.getAdvisedSupport(bean);
                    Advisor[] advisor = buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(null, null, null));
                    int pos;
                    for (Advisor avr : advisor) {
                        // Find the position based on the advisor's order, and add to advisors by pos
                        pos = findAddSeataAdvisorPosition(advised, avr);
                        advised.addAdvisor(pos, avr);
                    }
                }
                PROXYED_SET.add(beanName);
                return bean;
            }
        } catch (Exception exx) {
            throw new RuntimeException(exx);
        }
    }

    private boolean doCheckers(Object bean, String beanName) {
        if (PROXYED_SET.contains(beanName) || EXCLUDE_BEAN_NAME_SET.contains(beanName)
                || FactoryBean.class.isAssignableFrom(bean.getClass())) {
            return false;
        }

        if (!SCANNER_CHECKER_SET.isEmpty()) {
            for (ScannerChecker checker : SCANNER_CHECKER_SET) {
                try {
                    if (!checker.check(bean, beanName, beanFactory)) {
                        // failed check, do not scan this bean
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Do check failed: beanName={}, checker={}",
                            beanName, checker.getClass().getSimpleName(), e);
                }
            }
        }

        return true;
    }


    //region the methods about findAddSeataAdvisorPosition  START

    /**
     * Find pos for `advised.addAdvisor(pos, avr);`
     *
     * @param advised      the advised
     * @param seataAdvisor the seata advisor
     * @return the pos
     */
    private int findAddSeataAdvisorPosition(AdvisedSupport advised, Advisor seataAdvisor) {
        // Get seataAdvisor's order and interceptorPosition
        int seataOrder = OrderUtil.getOrder(seataAdvisor);
        SeataInterceptorPosition seataInterceptorPosition = getSeataInterceptorPosition(seataAdvisor);

        // If the interceptorPosition is any, check lowest or highest.
        if (SeataInterceptorPosition.Any == seataInterceptorPosition) {
            if (seataOrder == Ordered.LOWEST_PRECEDENCE) {
                // the last position
                return advised.getAdvisors().length;
            } else if (seataOrder == Ordered.HIGHEST_PRECEDENCE) {
                // the first position
                return 0;
            }
        } else {
            // If the interceptorPosition is not any, compute position if has TransactionInterceptor.
            Integer position = computePositionIfHasTransactionInterceptor(advised, seataAdvisor, seataInterceptorPosition, seataOrder);
            if (position != null) {
                // the position before or after TransactionInterceptor
                return position;
            }
        }

        // Find position
        return this.findPositionInAdvisors(advised.getAdvisors(), seataAdvisor);
    }

    @Nullable
    private Integer computePositionIfHasTransactionInterceptor(AdvisedSupport advised, Advisor seataAdvisor, SeataInterceptorPosition seataInterceptorPosition, int seataOrder) {
        // Find the TransactionInterceptor's advisor, order and position
        Advisor otherAdvisor = null;
        Integer transactionInterceptorPosition = null;
        Integer transactionInterceptorOrder = null;
        for (int i = 0, l = advised.getAdvisors().length; i < l; ++i) {
            otherAdvisor = advised.getAdvisors()[i];
            if (isTransactionInterceptor(otherAdvisor)) {
                transactionInterceptorPosition = i;
                transactionInterceptorOrder = OrderUtil.getOrder(otherAdvisor);
                break;
            }
        }
        // If the TransactionInterceptor does not exist, return null
        if (transactionInterceptorPosition == null) {
            return null;
        }

        // Reset seataOrder if the seataOrder is not match the position
        Advice seataAdvice = seataAdvisor.getAdvice();
        if (SeataInterceptorPosition.AfterTransaction == seataInterceptorPosition && OrderUtil.higherOrEquals(seataOrder, transactionInterceptorOrder)) {
            int newSeataOrder = OrderUtil.lower(transactionInterceptorOrder, 1);
            ((SeataInterceptor) seataAdvice).setOrder(newSeataOrder);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("The {}'s order '{}' is higher or equals than {}'s order '{}' , reset {}'s order to lower order '{}'.",
                        seataAdvice.getClass().getSimpleName(), seataOrder,
                        otherAdvisor.getAdvice().getClass().getSimpleName(), transactionInterceptorOrder,
                        seataAdvice.getClass().getSimpleName(), newSeataOrder);
            }
            // the position after the TransactionInterceptor's advisor
            return transactionInterceptorPosition + 1;
        } else if (SeataInterceptorPosition.BeforeTransaction == seataInterceptorPosition && OrderUtil.lowerOrEquals(seataOrder, transactionInterceptorOrder)) {
            int newSeataOrder = OrderUtil.higher(transactionInterceptorOrder, 1);
            ((SeataInterceptor) seataAdvice).setOrder(newSeataOrder);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("The {}'s order '{}' is lower or equals than {}'s order '{}' , reset {}'s order to higher order '{}'.",
                        seataAdvice.getClass().getSimpleName(), seataOrder,
                        otherAdvisor.getAdvice().getClass().getSimpleName(), transactionInterceptorOrder,
                        seataAdvice.getClass().getSimpleName(), newSeataOrder);
            }
            // the position before the TransactionInterceptor's advisor
            return transactionInterceptorPosition;
        }

        return null;
    }

    private int findPositionInAdvisors(Advisor[] advisors, Advisor seataAdvisor) {
        Advisor advisor;
        for (int i = 0, l = advisors.length; i < l; ++i) {
            advisor = advisors[i];
            if (OrderUtil.higherOrEquals(seataAdvisor, advisor)) {
                // the position before the current advisor
                return i;
            }
        }

        // the last position, after all the advisors
        return advisors.length;
    }

    private SeataInterceptorPosition getSeataInterceptorPosition(Advisor seataAdvisor) {
        Advice seataAdvice = seataAdvisor.getAdvice();
        if (seataAdvice instanceof SeataInterceptor) {
            return ((SeataInterceptor) seataAdvice).getPosition();
        } else {
            return SeataInterceptorPosition.Any;
        }
    }

    private boolean isTransactionInterceptor(Advisor advisor) {
        return SPRING_TRANSACTION_INTERCEPTOR_CLASS_NAME.equals(advisor.getAdvice().getClass().getName());
    }

    //endregion the methods about findAddSeataAdvisorPosition  END

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
            ConfigurationFactory.getInstance().addConfigListener(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, (CachedConfigurationChangeListener) this);
            return;
        }
        if (initialized.compareAndSet(false, true)) {
            initClient();
        }

        this.findBusinessBeanNamesNeededEnhancement();
    }

    private void findBusinessBeanNamesNeededEnhancement() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
            ConfigurableListableBeanFactory configurableListableBeanFactory = configurableApplicationContext.getBeanFactory();

            String[] beanNames = applicationContext.getBeanDefinitionNames();
            for (String contextBeanName : beanNames) {
                BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(contextBeanName);
                if (StringUtils.isBlank(beanDefinition.getBeanClassName())) {
                    continue;
                }
                if (IGNORE_ENHANCE_CHECK_SET.contains(beanDefinition.getBeanClassName())) {
                    continue;
                }
                try {
                    // get the class by bean definition class name
                    Class<?> beanClass = Class.forName(beanDefinition.getBeanClassName());
                    // check if it needs enhancement by the class
                    IfNeedEnhanceBean ifNeedEnhanceBean = DefaultInterfaceParser.get().parseIfNeedEnhancement(beanClass);
                    if (!ifNeedEnhanceBean.isIfNeed()) {
                        continue;
                    }
                    if (ifNeedEnhanceBean.getNeedEnhanceEnum().equals(NeedEnhanceEnum.SERVICE_BEAN)) {
                        // the native bean which dubbo, sofa bean service bean referenced
                        PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue("ref");
                        if (propertyValue == null) {
                            // the native bean which HSF service bean referenced
                            propertyValue = beanDefinition.getPropertyValues().getPropertyValue("target");
                        }
                        if (propertyValue != null) {
                            RuntimeBeanReference r = (RuntimeBeanReference) propertyValue.getValue();
                            if (r != null && StringUtils.isNotBlank(r.getBeanName())) {
                                NEED_ENHANCE_BEAN_NAME_SET.add(r.getBeanName());
                                continue;
                            }
                        }
                        // the native bean which local tcc service bean referenced
                        NEED_ENHANCE_BEAN_NAME_SET.add(contextBeanName);
                    } else if (ifNeedEnhanceBean.getNeedEnhanceEnum().equals(NeedEnhanceEnum.GLOBAL_TRANSACTIONAL_BEAN)) {
                        // global transactional bean
                        NEED_ENHANCE_BEAN_NAME_SET.add(contextBeanName);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("check if need enhance bean error, it can be ignore", e);
                }
            }
            LOGGER.info("The needed enhancement business beans are : {}", NEED_ENHANCE_BEAN_NAME_SET);
        }
    }

    private static final Set<String> IGNORE_ENHANCE_CHECK_SET = ImmutableSet.of(
            "org.apache.seata.spring.annotation.GlobalTransactionScanner"
            , "org.apache.seata.rm.fence.SpringFenceConfig"
            , "org.springframework.context.annotation.internalConfigurationAnnotationProcessor"
            , "org.springframework.context.annotation.internalAutowiredAnnotationProcessor"
            , "org.springframework.context.annotation.internalCommonAnnotationProcessor"
            , "org.springframework.context.event.internalEventListenerProcessor"
            , "org.springframework.context.event.internalEventListenerFactory"
    );


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        RemotingFactoryBeanParser remotingFactoryBeanParser = new RemotingFactoryBeanParser(applicationContext);
        DefaultRemotingParser.get().registerRemotingParser(remotingFactoryBeanParser);
        this.setBeanFactory(applicationContext);
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION.equals(event.getDataId())) {
            disableGlobalTransaction = Boolean.parseBoolean(event.getNewValue().trim());
            if (!disableGlobalTransaction && initialized.compareAndSet(false, true)) {
                LOGGER.info("{} config changed, old value:true, new value:{}", ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                        event.getNewValue());
                initClient();
                ConfigurationFactory.getInstance().removeConfigListener(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, this);
            }
        }
    }

    public static void setBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        GlobalTransactionScanner.beanFactory = beanFactory;
    }

    public static void addScannablePackages(String... packages) {
        PackageScannerChecker.addScannablePackages(packages);
    }

    public static void addScannerCheckers(Collection<ScannerChecker> scannerCheckers) {
        if (CollectionUtils.isNotEmpty(scannerCheckers)) {
            scannerCheckers.remove(null);
            SCANNER_CHECKER_SET.addAll(scannerCheckers);
        }
    }

    public static void addScannerCheckers(ScannerChecker... scannerCheckers) {
        if (ArrayUtils.isNotEmpty(scannerCheckers)) {
            addScannerCheckers(Arrays.asList(scannerCheckers));
        }
    }

    public static void addScannerExcludeBeanNames(String... beanNames) {
        if (ArrayUtils.isNotEmpty(beanNames)) {
            EXCLUDE_BEAN_NAME_SET.addAll(Arrays.asList(beanNames));
        }
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public static String getAccessKey() {
        return accessKey;
    }

    public static String getSecretKey() {
        return secretKey;
    }
}
